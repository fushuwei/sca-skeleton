package io.github.fushuwei.scaskeleton.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.github.fushuwei.scaskeleton.auth.config.properties.AuthLockProperties;
import io.github.fushuwei.scaskeleton.auth.config.properties.AuthJwtProperties;
import io.github.fushuwei.scaskeleton.auth.config.properties.OAuthClientsProperties;
import io.github.fushuwei.scaskeleton.auth.token.ScaOpaqueAccessTokenClaimsCustomizer;
import io.github.fushuwei.scaskeleton.auth.web.ClientAwareLoginUrlAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Spring Authorization Server 核心配置（Authorization Code + PKCE，无 Password Grant）。
 *
 * @author Fu Wei
 */
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@EnableConfigurationProperties({OAuthClientsProperties.class, AuthJwtProperties.class, AuthLockProperties.class})
public class AuthorizationServerConfig {

    /** OAuth2 客户端与 issuer 等对外 URL 配置（issuer 默认值见 {@code sca-skeleton-auth-*.yaml}） */
    private final OAuthClientsProperties oauthClientsProperties;

    /** RSA 密钥加载器（外部配置或内存生成） */
    private final AuthJwkKeyLoader authJwkKeyLoader;

    /**
     * SAS 标准端点过滤链：applyDefaultSecurity 负责 token 端点等默认放行规则。
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            OAuthClientsProperties oauthClientsProperties) throws Exception {

        // Spring Authorization Server 端点配置器（Boot 4 / Security 7 新写法，替代 applyDefaultSecurity）
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        http
            // 仅匹配 OAuth2 / OIDC 标准端点（显式 pattern，避免 configurer 未初始化时 matcher 为空）
            .securityMatcher("/oauth2/**", "/.well-known/**")
            .with(authorizationServerConfigurer, Customizer.withDefaults())
            .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
            .exceptionHandling(exceptions -> exceptions
                // 未登录访问 /oauth2/authorize 时，按 client_id 重定向到网关登录页（不用 MediaType 限制，避免 curl/浏览器 Accept 差异）
                .authenticationEntryPoint(new ClientAwareLoginUrlAuthenticationEntryPoint(
                        oauthClientsProperties,
                        oauthClientsProperties.resolveExternalLoginUrl(
                                oauthClientsProperties.getAdmin().getClientId())))
            )
            .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));

        // 启用 OIDC 端点（/.well-known/openid-configuration 等）
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .oidc(Customizer.withDefaults());

        return http.build();
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
        // 3) 委托生成器：按 RegisteredClient 的 token 格式选择具体生成器
        return new DelegatingOAuth2TokenGenerator(
                accessTokenGenerator, jwtGenerator, new OAuth2RefreshTokenGenerator());
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        // issuer 与登录重定向同源，均来自 sca.auth.issuer（YAML / AUTH_ISSUER）
        return AuthorizationServerSettings.builder()
                .issuer(oauthClientsProperties.getIssuer())
                .build();
    }
}
