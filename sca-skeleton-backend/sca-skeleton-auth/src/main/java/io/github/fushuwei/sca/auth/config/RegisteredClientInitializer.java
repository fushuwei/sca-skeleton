package io.github.fushuwei.sca.auth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 默认 OAuth2 注册客户端初始化器。
 * <p>
 * 服务启动时检查 {@code oauth2_registered_client} 表中是否存在 web 前端客户端，
 * 不存在则自动创建，避免首次部署时需要手动插库。
 * <p>
 * 客户端配置由 application.yml {@code sca.auth.client.web.*} 驱动，
 * 修改后重启服务生效（已存在的客户端不会被覆盖）。
 *
 * @author Fu Wei
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegisteredClientInitializer implements ApplicationRunner {

    private final RegisteredClientRepository registeredClientRepository;
    private final WebClientProperties webClientProperties;

    @Override
    public void run(ApplicationArguments args) {
        initWebClient();
    }

    private void initWebClient() {
        String clientId = webClientProperties.getClientId();
        if (registeredClientRepository.findByClientId(clientId) != null) {
            log.info("OAuth2 客户端 [{}] 已存在，跳过初始化", clientId);
            return;
        }

        RegisteredClient webClient = RegisteredClient
                .withId(java.util.UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret(webClientProperties.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                // 自定义密码授权模式
                .authorizationGrantType(PasswordGrantAuthenticationToken.PASSWORD)
                // 刷新令牌
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                // 预留授权码模式（future）
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://127.0.0.1:8080/login/oauth2/code/sca")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("all")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(
                                Duration.ofSeconds(webClientProperties.getAccessTokenTtl()))
                        .refreshTokenTimeToLive(
                                Duration.ofSeconds(webClientProperties.getRefreshTokenTtl()))
                        .reuseRefreshTokens(false)
                        .build())
                .build();

        registeredClientRepository.save(webClient);
        log.info("OAuth2 客户端 [{}] 初始化完成", clientId);
    }

    /**
     * Web 客户端配置属性（绑定 application.yml sca.auth.client.web.*）。
     */
    @Configuration
    @ConfigurationProperties(prefix = "sca.auth.client.web")
    public static class WebClientProperties {
        private String clientId = "sca-web-client";
        private String clientSecret = "{noop}sca-web-secret";
        private long accessTokenTtl = 7200;
        private long refreshTokenTtl = 604800;

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public long getAccessTokenTtl() { return accessTokenTtl; }
        public void setAccessTokenTtl(long accessTokenTtl) { this.accessTokenTtl = accessTokenTtl; }
        public long getRefreshTokenTtl() { return refreshTokenTtl; }
        public void setRefreshTokenTtl(long refreshTokenTtl) { this.refreshTokenTtl = refreshTokenTtl; }
    }
}
