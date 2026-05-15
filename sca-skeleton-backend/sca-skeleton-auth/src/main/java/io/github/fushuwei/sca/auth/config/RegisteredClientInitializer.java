package io.github.fushuwei.sca.auth.config;

import io.github.fushuwei.sca.auth.config.properties.WebClientProperties;
import io.github.fushuwei.sca.auth.extension.password.PasswordGrantAuthenticationToken;
import io.github.fushuwei.scaskeleton.core.uuid.UuidUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
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
 * 修改后重启服务生效（已存在的客户端不会被覆盖配置项，但会在启动时将会话访问令牌格式升级为 REFERENCE）。
 *
 * @author Fu Wei
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(WebClientProperties.class)
public class RegisteredClientInitializer implements ApplicationRunner {

    private final RegisteredClientRepository registeredClientRepository;
    private final WebClientProperties webClientProperties;

    @Override
    public void run(ApplicationArguments args) {
        initWebClient();
        migrateWebClientToOpaqueAccessTokenIfNeeded();
    }

    private void initWebClient() {
        String clientId = webClientProperties.getClientId();
        if (registeredClientRepository.findByClientId(clientId) != null) {
            log.info("OAuth2 客户端 [{}] 已存在，跳过初始化", clientId);
            return;
        }

        RegisteredClient webClient = RegisteredClient
                // 客户端主键使用全局统一 UUID（去连字符 32 位小写），满足主键策略约束
                .withId(UuidUtils.nextSimpleStr())
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
                        .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
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
     * 将已存在的 web 客户端访问令牌格式升级为不透明（REFERENCE），避免历史库仍为 JWT 自包含格式。
     */
    private void migrateWebClientToOpaqueAccessTokenIfNeeded() {
        String clientId = webClientProperties.getClientId();
        RegisteredClient client = registeredClientRepository.findByClientId(clientId);
        if (client == null) {
            return;
        }
        if (OAuth2TokenFormat.REFERENCE.equals(client.getTokenSettings().getAccessTokenFormat())) {
            return;
        }
        TokenSettings old = client.getTokenSettings();
        TokenSettings newSettings = TokenSettings.builder()
                .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                .accessTokenTimeToLive(old.getAccessTokenTimeToLive())
                .refreshTokenTimeToLive(old.getRefreshTokenTimeToLive())
                .reuseRefreshTokens(old.isReuseRefreshTokens())
                .authorizationCodeTimeToLive(old.getAuthorizationCodeTimeToLive())
                .deviceCodeTimeToLive(old.getDeviceCodeTimeToLive())
                .idTokenSignatureAlgorithm(old.getIdTokenSignatureAlgorithm())
                .x509CertificateBoundAccessTokens(old.isX509CertificateBoundAccessTokens())
                .build();
        registeredClientRepository.save(RegisteredClient.from(client).tokenSettings(newSettings).build());
        log.info("OAuth2 客户端 [{}] 已升级为不透明访问令牌（REFERENCE）", clientId);
    }

}
