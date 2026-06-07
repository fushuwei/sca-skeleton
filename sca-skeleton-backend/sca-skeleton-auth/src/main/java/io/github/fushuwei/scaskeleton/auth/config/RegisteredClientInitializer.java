package io.github.fushuwei.scaskeleton.auth.config;

import io.github.fushuwei.scaskeleton.auth.config.properties.OAuthClientsProperties;
import io.github.fushuwei.scaskeleton.core.uuid.UuidUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
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

    /** JDBC 模板：用于清理 DB 中格式损坏的历史客户端记录 */
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        // 清理 DB 中由旧 SQL 安装脚本写入的格式不兼容记录（JSON 缺少 @class 类型信息）
        repairCorruptedClientIfNeeded(oauthClientsProperties.getAdmin().getClientId());
        repairCorruptedClientIfNeeded(oauthClientsProperties.getPortal().getClientId());
        // 初始化管理后台公共客户端
        initPublicClientIfAbsent(oauthClientsProperties.getAdmin(), "SCA Admin SPA");
        // 初始化前台门户公共客户端
        initPublicClientIfAbsent(oauthClientsProperties.getPortal(), "SCA Portal SPA");
        // 若 redirect_uri 发生变更（如 nginx 统一入口改造），自动同步已有客户端
        syncRedirectUriIfChanged(oauthClientsProperties.getAdmin());
        syncRedirectUriIfChanged(oauthClientsProperties.getPortal());
        // 将历史客户端访问令牌格式迁移为 REFERENCE（不透明令牌）
        migrateToOpaqueAccessTokenIfNeeded(oauthClientsProperties.getAdmin().getClientId());
        migrateToOpaqueAccessTokenIfNeeded(oauthClientsProperties.getPortal().getClientId());
    }

    /**
     * 若 DB 中的客户端记录因 JSON 格式不兼容导致反序列化失败，先删除再让后续逻辑重建。
     * <p>
     * 触发场景：{@code deploy/sql/install/sca_platform.sql} 中的旧 JSON 不含 Jackson
     * {@code @class} 类型标识，与 SAS 7.0 的 {@code SecurityJacksonModules} 不兼容。
     */
    private void repairCorruptedClientIfNeeded(String clientId) {
        if (clientId == null) {
            return;
        }
        try {
            registeredClientRepository.findByClientId(clientId);
        } catch (Exception ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("@class")) {
                log.warn("OAuth2 客户端 [{}] 记录格式不兼容（缺少 @class），将在 DB 中删除后重建", clientId);
                jdbcTemplate.update("DELETE FROM oauth2_registered_client WHERE client_id = ?", clientId);
                // 清理 Redis 中的二级索引缓存（主键缓存因无主键无法精准删除，依赖 TTL 自然过期）
            } else {
                // 非预期异常重新抛出，避免静默吞掉真实错误
                throw new IllegalStateException("Failed to read OAuth2 client [" + clientId + "]: " + ex.getMessage(), ex);
            }
        }
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
                .scope("offline_access")
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
     * 若已存在客户端的 redirect_uri 与当前配置不一致，自动同步更新。
     * <p>
     * 解决 nginx 统一入口改造后 backend 配置已更新但 DB 中留存旧 Vite 直连地址的问题。
     *
     * @param props 当前配置中的客户端参数
     */
    private void syncRedirectUriIfChanged(OAuthClientsProperties.ClientProperties props) {
        if (props.getClientId() == null || props.getRedirectUri() == null) {
            return;
        }
        RegisteredClient existing = registeredClientRepository.findByClientId(props.getClientId());
        if (existing == null) {
            return;
        }
        String configuredUri = props.getRedirectUri();
        boolean hasUri = existing.getRedirectUris().contains(configuredUri);
        if (hasUri) {
            return;
        }
        log.warn("OAuth2 客户端 [{}] redirect_uri 不一致，当前 DB={}，配置={}，自动同步",
                props.getClientId(), existing.getRedirectUris(), configuredUri);
        RegisteredClient updated = RegisteredClient.from(existing)
                .redirectUris(uris -> {
                    uris.clear();
                    uris.add(configuredUri);
                })
                .build();
        registeredClientRepository.save(updated);
        log.info("OAuth2 客户端 [{}] redirect_uri 已同步为 {}", props.getClientId(), configuredUri);
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
