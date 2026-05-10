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
import io.github.fushuwei.sca.auth.token.ScaTokenCustomizer;
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
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
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
 *   <li>配置 RSA 密钥对用于 JWT 签名</li>
 *   <li>注入 ScaTokenCustomizer 向 JWT 写入业务 Claims</li>
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
                OAuth2AuthorizationServerConfigurer.authorizationServer();

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
     * Token 生成器：将 JWT 生成器、Refresh Token 生成器组合为代理链。
     * {@link ScaTokenCustomizer} 在 JWT 生成阶段被回调，写入业务 Claims。
     */
    @Bean
    public OAuth2TokenGenerator<?> tokenGenerator(JWKSource<SecurityContext> jwkSource) {
        JwtGenerator jwtGenerator = new JwtGenerator(
                new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(jwkSource));
        jwtGenerator.setJwtCustomizer(new ScaTokenCustomizer());
        return new DelegatingOAuth2TokenGenerator(jwtGenerator, new OAuth2RefreshTokenGenerator());
    }

    /**
     * 授权服务器全局设置：issuer 地址决定 JWT 的 iss claim，
     * 网关和资源服务通过此地址获取 JWK Set 完成验签。
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
