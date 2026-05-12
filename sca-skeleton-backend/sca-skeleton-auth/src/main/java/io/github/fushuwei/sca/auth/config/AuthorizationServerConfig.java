package io.github.fushuwei.sca.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.github.fushuwei.sca.auth.extension.password.PasswordGrantAuthenticationConverter;
import io.github.fushuwei.sca.auth.extension.password.PasswordGrantAuthenticationProvider;
import io.github.fushuwei.sca.auth.extension.password.PasswordGrantAuthenticationToken;
import io.github.fushuwei.sca.auth.security.ScaUserDetailsService;
import io.github.fushuwei.sca.auth.token.ScaOpaqueAccessTokenClaimsCustomizer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * Spring Authorization Server 核心配置。
 * <p>
 * 职责：
 * <ol>
 *   <li>配置授权服务器端点（/oauth2/token、/oauth2/jwks 等）</li>
 *   <li>注册自定义密码授权模式（Converter + Provider）</li>
 *   <li>配置 RSA 密钥对用于 OIDC id_token 等 JWT 场景</li>
 *   <li>注入不透明 access_token 生成链（REFERENCE）及业务 Claims</li>
 * </ol>
 *
 * @author Fu Wei
 */
@Configuration
@RequiredArgsConstructor
public class AuthorizationServerConfig {

    private final ScaUserDetailsService userDetailsService;
    private final OAuth2AuthorizationService authorizationService;

    /**
     * 授权服务器安全过滤链（最高优先级）。
     * 处理 /oauth2/token、/oauth2/jwks、/oauth2/introspect 等标准端点。
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            PasswordEncoder passwordEncoder,
            OAuth2TokenGenerator<?> tokenGenerator) throws Exception {

        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        http
            .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
            .with(authorizationServerConfigurer, configurer -> configurer
                // 注册自定义密码授权模式
                .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                    .accessTokenRequestConverter(new PasswordGrantAuthenticationConverter())
                    .authenticationProvider(new PasswordGrantAuthenticationProvider(
                            userDetailsService, passwordEncoder,
                            authorizationService, tokenGenerator))
                )
                // 开启 OIDC（/userinfo 端点）
                .oidc(Customizer.withDefaults())
            )
            .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
            // 未认证时跳转至登录页（授权码模式）；密码模式通过 API 直接调用
            .exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML))
            );

        return http.build();
    }

    /**
     * RSA 密钥对：用于 JWT 签名与验签。
     * 生产环境应从 KMS 或密钥文件加载，此处在内存中动态生成（服务重启 Token 失效）。
     * 替换方式：注入 @Value 读取 PEM/PKCS12 文件后构建 RSAKey。
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    /** JWT 解码器（授权服务器自身验证 token 时使用，如 introspect 端点）。 */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /**
     * Token 生成器：不透明 access_token（REFERENCE）+ JWT（如 id_token）+ refresh_token 的委托链。
     * 访问令牌业务字段由 {@link ScaOpaqueAccessTokenClaimsCustomizer} 写入，经自省返回给资源服务器。
     */
    @Bean
    public OAuth2TokenGenerator<?> tokenGenerator(JWKSource<SecurityContext> jwkSource) {
        OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
        accessTokenGenerator.setAccessTokenCustomizer(new ScaOpaqueAccessTokenClaimsCustomizer());
        JwtGenerator jwtGenerator = new JwtGenerator(
                new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(jwkSource));
        return new DelegatingOAuth2TokenGenerator(
                accessTokenGenerator, jwtGenerator, new OAuth2RefreshTokenGenerator());
    }

    /**
     * 授权服务器全局设置：issuer 用于访问令牌 claims 与 OIDC 元数据；
     * 资源服务器通过自省校验不透明令牌，不再依赖 JWK 验签 access_token。
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://sca-skeleton-auth:9000")
                .build();
    }

    private KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("RSA 密钥对生成失败", ex);
        }
    }
}
