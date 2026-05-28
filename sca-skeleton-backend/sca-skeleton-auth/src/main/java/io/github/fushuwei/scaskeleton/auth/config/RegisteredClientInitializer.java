package io.github.fushuwei.scaskeleton.auth.config;

import io.github.fushuwei.scaskeleton.auth.config.properties.OAuthClientsProperties;
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
 * OAuth2 公共客户端初始化器：为 admin / portal 两个 SPA 注册 Authorization Code + PKCE 客户端。
 * <p>
 * 仅在 {@code oauth2_registered_client} 中不存在对应 {@code client_id} 时插入，生产环境建议改为运维预置 SQL。
 *
 * @author Fu Wei
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(OAuthClientsProperties.class)
public class RegisteredClientInitializer implements ApplicationRunner {

    /** 授权码有效期：缩短窗口降低 code interception 风险。 */
    private static final Duration AUTHORIZATION_CODE_TTL = Duration.ofSeconds(60);

    /** JDBC + Redis 缓存的客户端仓库 */
    private final RegisteredClientRepository registeredClientRepository;

    /** admin / portal 客户端外部化配置 */
    private final OAuthClientsProperties oauthClientsProperties;

    @Override
    public void run(ApplicationArguments args) {
        // 初始化管理后台公共客户端
        initPublicClientIfAbsent(oauthClientsProperties.getAdmin(), "SCA Admin SPA");
        // 初始化前台门户公共客户端
        initPublicClientIfAbsent(oauthClientsProperties.getPortal(), "SCA Portal SPA");
        // 将历史客户端访问令牌格式迁移为 REFERENCE（不透明令牌）
        migrateToOpaqueAccessTokenIfNeeded(oauthClientsProperties.getAdmin().getClientId());
        migrateToOpaqueAccessTokenIfNeeded(oauthClientsProperties.getPortal().getClientId());
    }

    /**
     * 若 client_id 不存在则注册公共客户端（无 client_secret，强制 PKCE）。
     *
     * @param props      客户端配置项
     * @param clientName 可读名称，写入 client_name 字段
     */
    private void initPublicClientIfAbsent(OAuthClientsProperties.ClientProperties props, String clientName) {
        // 配置缺失时跳过，避免写入空 client_id
        if (props.getClientId() == null || props.getRedirectUri() == null) {
            log.warn("OAuth2 客户端配置不完整，跳过初始化：clientId={}", props.getClientId());
            return;
        }
        // 已存在则不再覆盖（生产由运维预置）
        if (registeredClientRepository.findByClientId(props.getClientId()) != null) {
            log.info("OAuth2 客户端 [{}] 已存在，跳过初始化", props.getClientId());
            return;
        }
        RegisteredClient client = RegisteredClient
                // 主键使用全局 UUID 策略
                .withId(UuidUtils.nextSimpleStr())
                .clientId(props.getClientId())
                .clientName(clientName)
                // 公共客户端：不进行 client_secret 认证
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                // 授权码 + 刷新令牌
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                // PKCE 回调地址（必须与 SPA 环境变量一致）
                .redirectUri(props.getRedirectUri())
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("all")
                .clientSettings(ClientSettings.builder()
                        // OAuth 2.1：公共客户端强制 PKCE
                        .requireProofKey(true)
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                        .authorizationCodeTimeToLive(AUTHORIZATION_CODE_TTL)
                        .accessTokenTimeToLive(Duration.ofSeconds(props.getAccessTokenTtl()))
                        .refreshTokenTimeToLive(Duration.ofSeconds(props.getRefreshTokenTtl()))
                        .reuseRefreshTokens(false)
                        .build())
                .build();
        registeredClientRepository.save(client);
        log.info("OAuth2 公共客户端 [{}] 初始化完成，redirect_uri={}", props.getClientId(), props.getRedirectUri());
    }

    /**
     * 将已存在客户端的 access_token 格式升级为 REFERENCE（不透明令牌）。
     *
     * @param clientId 目标 client_id
     */
    private void migrateToOpaqueAccessTokenIfNeeded(String clientId) {
        if (clientId == null) {
            return;
        }
        RegisteredClient client = registeredClientRepository.findByClientId(clientId);
        if (client == null) {
            return;
        }
        // 已是 REFERENCE 则无需迁移
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
