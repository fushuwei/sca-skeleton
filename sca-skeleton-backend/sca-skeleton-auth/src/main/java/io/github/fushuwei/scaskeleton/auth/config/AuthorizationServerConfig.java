package io.github.fushuwei.scaskeleton.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.github.fushuwei.scaskeleton.auth.config.properties.AuthLockProperties;
import io.github.fushuwei.scaskeleton.auth.config.properties.AuthJwtProperties;
import io.github.fushuwei.scaskeleton.auth.config.properties.AuthLoginProperties;
import io.github.fushuwei.scaskeleton.auth.config.properties.OAuth2ClientProperties;
import io.github.fushuwei.scaskeleton.auth.jwk.AuthJwkKeyLoader;
import io.github.fushuwei.scaskeleton.auth.security.filter.AuthorizeChannelIsolationFilter;
import io.github.fushuwei.scaskeleton.auth.security.filter.PublicClientRefreshTokenAuthenticationFilter;
import io.github.fushuwei.scaskeleton.auth.token.ScaOpaqueAccessTokenClaimsCustomizer;
import io.github.fushuwei.scaskeleton.auth.token.ScaRefreshTokenGenerator;
import io.github.fushuwei.scaskeleton.auth.security.handler.ClientAwareLoginUrlAuthenticationEntryPoint;
import io.github.fushuwei.scaskeleton.auth.security.OAuth2PendingAuthorizeStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;

/**
 * Spring Authorization Server 核心配置（基于 OAuth 2.1 实现 Authorization Code + PKCE）
 *
 * @author Fu Wei
 */
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@EnableConfigurationProperties({OAuth2ClientProperties.class, AuthJwtProperties.class, AuthLockProperties.class, AuthLoginProperties.class})
public class AuthorizationServerConfig {

    /**
     * OAuth2 客户端与 issuer 等对外 URL 配置（issuer 默认值见 {@code sca-skeleton-auth-*.yaml}）
     */
    private final OAuth2ClientProperties oauth2ClientProperties;

    /**
     * RSA 密钥加载器（外部配置或内存生成）
     */
    private final AuthJwkKeyLoader authJwkKeyLoader;

    /**
     * 与表单登录链共享的 SavedRequest 缓存
     */
    private final HttpSessionRequestCache httpSessionRequestCache;

    /**
     * pending authorize Session 存储
     */
    private final OAuth2PendingAuthorizeStore pendingAuthorizeStore;
    /**
     * 授权端点渠道隔离过滤器（阻断 admin/portal 静默串登）
     */
    private final AuthorizeChannelIsolationFilter authorizeChannelIsolationFilter;

    /**
     * 注册客户端仓库（JDBC + Redis 缓存），供公共客户端 refresh_token 认证过滤器使用
     */
    private final RegisteredClientRepository registeredClientRepository;

    /**
     * 公共客户端 refresh_token 认证过滤器：
     * SAS 7.1.x 的 PublicClientAuthenticationConverter 仅匹配 PKCE 请求
     * （grant_type=authorization_code + code_verifier），对 refresh_token grant
     * 返回 null → 客户端认证失败 → OAuth2RefreshTokenAuthenticationConverter
     * 从 SecurityContext 读取不到 clientPrincipal → 401。
     * <p>
     * 本过滤器在 SAS 默认认证之前将公共客户端身份写入 SecurityContext。
     */
    @Bean
    public PublicClientRefreshTokenAuthenticationFilter publicClientRefreshTokenAuthenticationFilter() {
        return new PublicClientRefreshTokenAuthenticationFilter(registeredClientRepository);
    }

    /**
     * 未登录访问 authorize 时的登录入口（按 client_id 分流 admin / portal 登录页）
     */
    @Bean
    public ClientAwareLoginUrlAuthenticationEntryPoint clientAwareLoginUrlAuthenticationEntryPoint() {
        return new ClientAwareLoginUrlAuthenticationEntryPoint(
            oauth2ClientProperties,
            pendingAuthorizeStore,
            oauth2ClientProperties.resolveExternalLoginUrl(
                oauth2ClientProperties.getAdmin().getClientId()));
    }

    /**
     * SAS 标准端点过滤链：applyDefaultSecurity 负责 token 端点等默认放行规则
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
        HttpSecurity http,
        ClientAwareLoginUrlAuthenticationEntryPoint clientAwareLoginUrlAuthenticationEntryPoint,
        PublicClientRefreshTokenAuthenticationFilter publicClientRefreshTokenAuthenticationFilter) throws Exception {

        // Spring Authorization Server 端点配置器（Boot 4 / Security 7 新写法，替代 applyDefaultSecurity）
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
            new OAuth2AuthorizationServerConfigurer();

        http
            // 仅匹配 OAuth2 / OIDC 标准端点（显式 pattern，避免 configurer 未初始化时 matcher 为空）
            .securityMatcher("/oauth2/**", "/.well-known/**")
            // 显式禁用 CSRF：token / introspection / revocation 等机器端点不应要求 CSRF Token
            .csrf(csrf -> csrf.disable())
            .with(authorizationServerConfigurer, Customizer.withDefaults())
            .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
            // 在匿名认证前执行：
            // ① 公共客户端 refresh_token 认证（补偿 SAS 7.1.x PublicClientAuthenticationConverter 不匹配 refresh_token grant）
            // ② 渠道隔离校验（已登录但渠道不匹配时清空会话并触发重新登录）
            .addFilterBefore(publicClientRefreshTokenAuthenticationFilter, AnonymousAuthenticationFilter.class)
            .addFilterBefore(authorizeChannelIsolationFilter, AnonymousAuthenticationFilter.class)
            .requestCache(cache -> cache.requestCache(httpSessionRequestCache))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .exceptionHandling(exceptions -> exceptions
                // 仅 authorize 端点跳转登录页
                .defaultAuthenticationEntryPointFor(
                    clientAwareLoginUrlAuthenticationEntryPoint,
                    oauth2AuthorizeEndpointMatcher())
                // token / revocation 等机器端点返回标准 OAuth2 JSON 错误（含 error/error_description），
                // 避免 SAS 默认的 Http403ForbiddenEntryPoint 返回空 body 401/403
                .defaultAuthenticationEntryPointFor(
                    oauth2TokenEndpointAuthenticationEntryPoint(),
                    oauth2TokenEndpointMatcher())
            );

        // 启用 OIDC 端点（/.well-known/openid-configuration 等）
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .oidc(Customizer.withDefaults());

        return http.build();
    }

    /**
     * 仅匹配浏览器授权端点（GET 发起授权、POST 提交 consent），不含 token 等机器端点
     */
    private static RequestMatcher oauth2AuthorizeEndpointMatcher() {
        return new OrRequestMatcher(
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/oauth2/authorize"),
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/oauth2/authorize"));
    }

    /**
     * 匹配机器端点（token / revocation），需返回 OAuth2 JSON 错误而非空 body 401
     */
    private static RequestMatcher oauth2TokenEndpointMatcher() {
        return new OrRequestMatcher(
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/oauth2/token"),
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/oauth2/revoke"),
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/oauth2/introspect"));
    }

    /**
     * Token 端点专用 AuthenticationEntryPoint：返回标准 OAuth2 JSON 错误响应，
     * 包含 {@code error} 和 {@code error_description} 字段，便于调用方解析。
     * <p>
     * 解决 SAS 默认 {@code Http403ForbiddenEntryPoint} 对 token/revoke 端点
     * 只返回空 body 401/403 的问题——这对 SPA 的静默 refresh 是不可调试的。
     */
    private AuthenticationEntryPoint oauth2TokenEndpointAuthenticationEntryPoint() {
        return (HttpServletRequest request, HttpServletResponse response,
                AuthenticationException authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            // 手工构造 JSON，完整转义特殊字符
            String rawMessage = authException.getMessage() != null
                ? authException.getMessage()
                : "Unauthorized";
            String message = rawMessage
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
            String json = String.format(
                "{\"error\":\"unauthorized\",\"error_description\":\"%s\",\"timestamp\":%d}",
                message, Instant.now().toEpochMilli());
            try {
                response.getWriter().write(json);
            } catch (IOException e) {
                // 写入失败时仅结束响应
            }
        };
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        // 1) 优先加载外部持久化 RSA 密钥，否则本地内存生成
        KeyPair keyPair = authJwkKeyLoader.loadKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        // 2) 构建 RSA JWK，kid 外部配置优先
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(authJwkKeyLoader.resolveKeyId())
            .build();
        // 3) 封装为不可变 JWK 源，供 JWT 编码与 JWKS 端点使用
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public OAuth2TokenGenerator<?> tokenGenerator(JWKSource<SecurityContext> jwkSource) {
        // 1) 不透明 access_token 生成器，挂载业务 claims 扩展
        OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
        accessTokenGenerator.setAccessTokenCustomizer(new ScaOpaqueAccessTokenClaimsCustomizer());
        // 2) JWT 生成器（用于 OIDC id_token 等场景，非默认 access_token 格式）
        JwtGenerator jwtGenerator = new JwtGenerator(
            new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(jwkSource));
        // 3) refresh_token 生成器：使用自定义实现，允许向公共客户端签发 refresh_token（SAS 7.0 默认会拒绝）
        // 4) 委托生成器：按 RegisteredClient 的 token 格式选择具体生成器
        return new DelegatingOAuth2TokenGenerator(
            accessTokenGenerator, jwtGenerator, new ScaRefreshTokenGenerator());
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        // issuer 与登录重定向同源，均来自 sca.auth.issuer（YAML / AUTH_ISSUER）
        return AuthorizationServerSettings.builder()
            .issuer(oauth2ClientProperties.getIssuer())
            .build();
    }
}
