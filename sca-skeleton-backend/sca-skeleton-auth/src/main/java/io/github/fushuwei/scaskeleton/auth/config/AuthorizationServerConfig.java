package io.github.fushuwei.scaskeleton.auth.config;

import io.github.fushuwei.scaskeleton.auth.config.properties.AuthLockProperties;
import io.github.fushuwei.scaskeleton.auth.config.properties.OAuth2ClientProperties;
import io.github.fushuwei.scaskeleton.auth.grant.password.OAuth2ResourceOwnerPasswordAuthenticationConverter;
import io.github.fushuwei.scaskeleton.auth.grant.password.OAuth2ResourceOwnerPasswordAuthenticationProvider;
import io.github.fushuwei.scaskeleton.auth.security.RoutingUserDetailsService;
import io.github.fushuwei.scaskeleton.auth.security.filter.CaptchaVerificationFilter;
import io.github.fushuwei.scaskeleton.auth.security.handler.OAuth2TokenEndpointFailureHandler;
import io.github.fushuwei.scaskeleton.auth.token.ScaOpaqueAccessTokenClaimsCustomizer;
import io.github.fushuwei.scaskeleton.captcha.CaptchaService;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.core.result.ResultType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2ClientCredentialsAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2RefreshTokenAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;

/**
 * Spring Authorization Server 核心配置（密码模式扩展）
 * <p>
 * 基于 SAS 扩展自定义的 Resource Owner Password Credentials Grant，
 * 保留 SAS 原生的 refresh_token、client_credentials 支持。
 * <p>
 * 认证流程：
 * <ol>
 *   <li>SPA 发送 {@code POST /oauth2/token} 携带 {@code grant_type=password&username&password} 和 Basic 认证头</li>
 *   <li>SAS 客户端认证过滤器校验 client_id / client_secret（client_secret_basic）</li>
 *   <li>验证码过滤器从 SecurityContext 读取已认证客户端，portal 渠道校验图形验证码</li>
 *   <li>密码模式转换器构建 {@code OAuth2ResourceOwnerPasswordAuthenticationToken}</li>
 *   <li>密码模式提供者委托 {@link AuthenticationManager} 完成用户认证</li>
 *   <li>认证成功/失败时直接发布 Spring Security 事件（携带原始 UsernamePasswordAuthenticationToken），
 *       驱动登录日志与账号锁定</li>
 *   <li>生成不透明 access_token + refresh_token 并返回</li>
 * </ol>
 *
 * @author Fu Wei
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@EnableConfigurationProperties({OAuth2ClientProperties.class, AuthLockProperties.class})
public class AuthorizationServerConfig {

    private final OAuth2ClientProperties oauth2ClientProperties;
    private final OAuth2AuthorizationService authorizationService;
    private final RoutingUserDetailsService routingUserDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final CaptchaVerificationFilter captchaVerificationFilter;
    private final OAuth2TokenEndpointFailureHandler oauth2TokenEndpointFailureHandler;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * JSON 序列化器（注入容器中的全局 {@link JsonMapper} Bean）。
     * <p>
     * 由 {@code sca-skeleton-starter-core} 的 {@code JacksonAutoConfiguration} 注册，
     * 已配置统一的时区、JavaTimeModule 等序列化策略。
     */
    private final JsonMapper jsonMapper;

    /**
     * SAS 标准端点 + 密码模式扩展过滤链
     * <p>
     * Order=1：仅匹配 OAuth2 标准端点（token / revoke / introspect / jwk）
     * <p>
     * 不匹配 {@code /.well-known/**}：项目使用不透明令牌 + Redis 自省，无 JWT 验签 / OIDC discovery 需求，
     * discovery 端点不对外暴露。securityMatcher 不匹配 → {@code OAuth2AuthorizationServerMetadataEndpointFilter}
     * 不运行 → discovery 文档不生成 → 请求落入 Order=2 链被拦截（经网关 401，直连 403），
     * 比仅移除网关白名单更彻底（白名单只挡外部访问，securityMatcher 连内部 filter 也不注册）。
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
            new OAuth2AuthorizationServerConfigurer();

        http
            .securityMatcher("/oauth2/**")
            .csrf(AbstractHttpConfigurer::disable)
            .with(authorizationServerConfigurer, configurer -> configurer
                .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                    // 注册密码模式转换器（与 SAS 原生转换器组合为委托模式）
                    .accessTokenRequestConverter(accessTokenRequestConverter())
                    // 自定义认证失败处理器：返回统一 Result 格式（HTTP 200），替代 SAS 默认的
                    // OAuth2ErrorAuthenticationFailureHandler（HTTP 400 + error/error_description）
                    .errorResponseHandler(oauth2TokenEndpointFailureHandler)
                )
            )
            .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
            // 验证码校验：在用户名密码认证前拦截 token 端点的密码模式请求
            .addFilterBefore(captchaVerificationFilter, UsernamePasswordAuthenticationFilter.class)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 注册密码模式认证提供者
            .authenticationProvider(passwordAuthenticationProvider())
            .exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                    oauth2TokenEndpointAuthenticationEntryPoint(),
                    request -> "POST".equalsIgnoreCase(request.getMethod())
                        && "/oauth2/token".equals(request.getServletPath()))
            );

        return http.build();
    }

    /**
     * token 端点请求转换器：SAS 原生转换器 + 密码模式转换器的委托组合
     * <p>
     * SAS 按列表顺序依次调用每个转换器的 {@code convert()} 方法，
     * 第一个返回非 null 的转换器结果将被用于后续认证。
     * <p>
     * 已移除 {@code OAuth2AuthorizationCodeAuthenticationConverter}（密码模式不再需要授权码流程）。
     */
    private AuthenticationConverter accessTokenRequestConverter() {
        List<AuthenticationConverter> converters = List.of(
            new OAuth2ClientCredentialsAuthenticationConverter(),
            new OAuth2RefreshTokenAuthenticationConverter(),
            new OAuth2ResourceOwnerPasswordAuthenticationConverter()
        );
        // 委托模式：按顺序调用每个转换器，返回第一个非 null 结果
        return request -> {
            for (AuthenticationConverter converter : converters) {
                Authentication authentication = converter.convert(request);
                if (authentication != null) {
                    return authentication;
                }
            }
            return null;
        };
    }

    /**
     * 密码模式认证提供者
     * <p>
     * 委托 {@link #authenticationManager()} 完成用户名密码认证，
     * 认证成功后生成 access_token 和 refresh_token。
     * 认证成功/失败时直接发布 Spring Security 事件，驱动登录日志与账号锁定。
     */
    private OAuth2ResourceOwnerPasswordAuthenticationProvider passwordAuthenticationProvider() {
        return new OAuth2ResourceOwnerPasswordAuthenticationProvider(
            authenticationManager(),
            authorizationService,
            tokenGenerator(),
            applicationEventPublisher,
            oauth2ClientProperties
        );
    }

    /**
     * 用户认证管理器：基于 DaoAuthenticationProvider + RoutingUserDetailsService
     * <p>
     * 供密码模式 Provider 内部调用 {@code authenticationManager.authenticate()} 完成用户认证。
     * {@link DaoAuthenticationProvider} 使用 {@link RoutingUserDetailsService} 按
     * {@link io.github.fushuwei.scaskeleton.auth.security.LoginChannel} 路由加载用户。
     * <p>
     * 注意：认证成功/失败事件由 {@link io.github.fushuwei.scaskeleton.auth.grant.base.OAuth2ResourceOwnerBaseAuthenticationProvider}
     * 直接发布（携带原始 UsernamePasswordAuthenticationToken），因此此处 ProviderManager 无需注入事件发布器。
     * <p>
     * {@code DaoAuthenticationProvider} 默认 {@code hideUserNotFoundExceptions=true}，
     * 会将 {@code UsernameNotFoundException} 转换为 {@code BadCredentialsException}，
     * 防止用户名枚举攻击。此处保持默认行为，不调用 {@code setHideUserNotFoundExceptions(false)}。
     */
    @SuppressWarnings("deprecation") // Spring Security 6.x DaoAuthenticationProvider(UserDetailsService) 构造器已废弃，但当前版本尚不支持无参构造 + setUserDetailsService()
    private AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(routingUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * Token 端点专用 AuthenticationEntryPoint：返回统一的 {@link Result} 格式响应（HTTP 200）
     * <p>
     * 处理 token 端点的客户端认证失败（如 client_id/client_secret 错误）。
     * 使用 Jackson {@link tools.jackson.databind.json.JsonMapper} 序列化，确保 JSON 转义正确。
     * 必须显式设置 UTF-8 字符编码，否则中文 message 会变成乱码。
     */
    private AuthenticationEntryPoint oauth2TokenEndpointAuthenticationEntryPoint() {
        return (HttpServletRequest request, HttpServletResponse response,
                org.springframework.security.core.AuthenticationException authException) -> {
            String message = authException.getMessage() != null
                ? authException.getMessage() : "认证失败";
            Result<Void> result = Result.of(ResultCode.FAILURE.getCode(), message, null, ResultType.FAILURE);
            response.setStatus(HttpStatus.OK.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            try {
                response.getWriter().write(jsonMapper.writeValueAsString(result));
            } catch (IOException e) {
                log.warn("写入 token 端点错误响应失败", e);
            }
        };
    }

    /**
     * OAuth2 令牌生成器
     * <p>
     * 1) 不透明 access_token 生成器（REFERENCE 格式），挂载业务 claims 扩展
     * 2) SAS 内置 refresh_token 生成器（机密客户端可直接使用，无需自定义实现）
     * 3) 委托生成器：按 RegisteredClient 的 token 格式选择具体生成器
     */
    @Bean
    public OAuth2TokenGenerator<?> tokenGenerator() {
        OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
        accessTokenGenerator.setAccessTokenCustomizer(new ScaOpaqueAccessTokenClaimsCustomizer());
        // SAS 内置 refresh_token 生成器对机密客户端正常工作
        OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();
        return new DelegatingOAuth2TokenGenerator(accessTokenGenerator, refreshTokenGenerator);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        log.info("AuthorizationServer 初始化完成：issuer={}", oauth2ClientProperties.getIssuer());
        return AuthorizationServerSettings.builder()
            .issuer(oauth2ClientProperties.getIssuer())
            .build();
    }
}
