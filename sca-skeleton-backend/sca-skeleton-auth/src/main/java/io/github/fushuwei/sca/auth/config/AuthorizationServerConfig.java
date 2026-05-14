package io.github.fushuwei.sca.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.github.fushuwei.sca.auth.extension.password.PasswordGrantAuthenticationConverter;
import io.github.fushuwei.sca.auth.extension.password.PasswordGrantAuthenticationProvider;
import io.github.fushuwei.sca.auth.security.ScaUserDetailsService;
import io.github.fushuwei.sca.auth.token.ScaOpaqueAccessTokenClaimsCustomizer;
import io.github.fushuwei.scaskeleton.core.uuid.UuidUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
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

/**
 * Spring Authorization Server 核心配置。
 * <p>
 * 本项目认证中心基于 Spring Authorization Server starter（Spring Boot 4 内置工件
 * {@code spring-boot-starter-security-oauth2-authorization-server}），以标准 OAuth2 协议提供
 * {@code /oauth2/token}、{@code /oauth2/introspect}、{@code /oauth2/revoke}、{@code /oauth2/jwks} 等端点。
 * <p>
 * 配置职责：
 * <ol>
 *   <li>启用授权服务器标准端点与对应安全过滤链（最高优先级）</li>
 *   <li>注册自定义 password 授权模式扩展（Converter + Provider）</li>
 *   <li>提供 JWK（仅用于 OIDC id_token 等 JWT 场景；access_token 为不透明令牌不依赖 JWK）</li>
 *   <li>装配不透明 access_token 生成链（REFERENCE 格式）+ 业务 Claims customizer + refresh_token 生成器</li>
 *   <li>统一通过外部化配置注入 issuer，禁止硬编码到代码</li>
 * </ol>
 *
 * @author Fu Wei
 */
@Configuration
@RequiredArgsConstructor
public class AuthorizationServerConfig {

    /**
     * 用户加载服务：自定义 password 授权 Provider 通过它加载并校验用户。
     */
    private final ScaUserDetailsService userDetailsService;

    /**
     * 授权记录存储：自定义 password 授权 Provider 在颁发令牌后将授权写入此存储（项目实现为 Redis）。
     */
    private final OAuth2AuthorizationService authorizationService;

    /**
     * 授权服务器对外发布的 issuer，外部化配置注入；OIDC 元数据与令牌 claims 中均使用该值。
     */
    @Value("${sca.auth.issuer:http://localhost:9000}")
    private String issuer;

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
     * RSA 密钥对：仅用于 OIDC id_token 等 JWT 场景的签名与验签；不透明 access_token 不依赖 JWK。
     * <p>
     * 生产环境必须从 KMS 或外部密钥文件加载，此处在内存中动态生成会导致服务重启后旧 id_token 全部失效，
     * 仅适合本地开发与 CI 冒烟验证。替换方式：注入 PEM/PKCS12 文件路径后构建 RSAKey。
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                // 密钥 ID 统一使用 32 位 UUID 字符串，遵循全局 ID 策略
                .keyID(UuidUtils.nextSimpleStr())
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
     * 授权服务器全局设置：issuer 写入 OIDC discovery 元数据与 JWT 类令牌 claims；
     * 资源服务器通过 {@code /oauth2/introspect} 校验不透明 access_token，不依赖 JWK 验签。
     * <p>
     * issuer 通过外部化配置注入（{@code sca.auth.issuer}），不同环境（本地/容器/网关回环）应在
     * application-{profile}.yml 或环境变量中覆盖，禁止硬编码到代码中。
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
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
