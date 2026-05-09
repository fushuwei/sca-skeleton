package io.github.fushuwei.sca.auth.bootstrap;

import io.github.fushuwei.sca.auth.config.ScaAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * 启动时确保默认 OAuth2 客户端存在，与网关及前端授权码模式对齐。
 *
 * @author Fu Wei
 */
@Component
@RequiredArgsConstructor
public class RegisteredClientInitializer implements ApplicationRunner {

    // 持久化客户端。
    private final RegisteredClientRepository registeredClientRepository;
    // 口令编码，client_secret 落库需带 {bcrypt} 等前缀。
    private final PasswordEncoder passwordEncoder;
    // 与自定义 JWT 对齐的 TTL。
    private final ScaAuthProperties authProperties;

    // 默认网关客户端 ID，供授权码跳转配置引用。
    public static final String DEFAULT_GATEWAY_CLIENT_ID = "sca-gateway";

    @Override
    public void run(ApplicationArguments args) {
        // 已存在则跳过，避免覆盖运维手工配置。
        if (registeredClientRepository.findByClientId(DEFAULT_GATEWAY_CLIENT_ID) != null) {
            return;
        }
        // 生产环境务必通过环境变量覆盖默认密钥。
        String encodedSecret = passwordEncoder.encode("change-me-gateway-client-secret");
        // 构建注册客户端，字段与 oauth2_registered_client 表一一对应由 JDBC 仓库序列化。
        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(DEFAULT_GATEWAY_CLIENT_ID)
                .clientName("SCA Gateway / SPA")
                .clientSecret(encodedSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://127.0.0.1:9999/login/oauth2/code/sca-gateway")
                .postLogoutRedirectUri("http://127.0.0.1:9999/")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofSeconds(authProperties.getAccessTokenTtlSeconds()))
                        .refreshTokenTimeToLive(Duration.ofSeconds(authProperties.getRefreshTokenTtlSeconds()))
                        .reuseRefreshTokens(false)
                        .build())
                .build();
        // 写入数据库。
        registeredClientRepository.save(registeredClient);
    }
}
