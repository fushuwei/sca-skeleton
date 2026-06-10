package io.github.fushuwei.scaskeleton.auth.initializer;

import io.github.fushuwei.scaskeleton.auth.config.properties.OAuth2ClientProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
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
 * OAuth2 内置公共客户端初始化程序
 *
 * @author Fu Wei
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2RegisteredClientInitializer implements ApplicationRunner {

    /**
     * 授权码有效期
     */
    private static final Duration AUTHORIZATION_CODE_TTL = Duration.ofSeconds(60);

    /**
     * JDBC + Redis 缓存的客户端存储库
     */
    private final RegisteredClientRepository registeredClientRepository;

    /**
     * 公共客户端配置属性
     */
    private final OAuth2ClientProperties oauth2ClientProperties;

    /**
     * JDBC 模板
     */
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        // 清理 DB 中由旧 SQL 安装脚本写入的格式不兼容记录（JSON 缺少 @class 类型信息）
        repairCorruptedClientIfNeeded(oauth2ClientProperties.getAdmin().getClientId());
        repairCorruptedClientIfNeeded(oauth2ClientProperties.getPortal().getClientId());
        // 初始化管理后台公共客户端
        initPublicClientIfAbsent(oauth2ClientProperties.getAdmin(), "SCA Admin SPA");
        // 初始化前台门户公共客户端
        initPublicClientIfAbsent(oauth2ClientProperties.getPortal(), "SCA Portal SPA");
        // 若 redirect_uri 发生变更（如 nginx 统一入口改造），自动同步已有客户端
        syncRedirectUriIfChanged(oauth2ClientProperties.getAdmin());
        syncRedirectUriIfChanged(oauth2ClientProperties.getPortal());
        // 将历史客户端访问令牌格式迁移为 REFERENCE（不透明令牌）
        migrateToOpaqueAccessTokenIfNeeded(oauth2ClientProperties.getAdmin().getClientId());
        migrateToOpaqueAccessTokenIfNeeded(oauth2ClientProperties.getPortal().getClientId());
    }

    /**
     * 若 DB 中的客户端记录因 JSON 格式不兼容导致反序列化失败，先删除再让后续逻辑重建
     *
     * @param clientId OAuth2 client_id
     */
    private void repairCorruptedClientIfNeeded(String clientId) {
        if (clientId == null) {
            return;
        }
        try {
            registeredClientRepository.findByClientId(clientId);
        } catch (Exception ex) {
            log.warn("OAuth2 客户端 [{}] 记录格式不兼容，将在 DB 中删除后重建：{}", clientId, ex.getMessage());
            jdbcTemplate.update("DELETE FROM oauth2_registered_client WHERE client_id = ?", clientId);
        }
    }

    /**
     * 若 client_id 不存在则注册公共客户端（无 client_secret，强制 PKCE）
     *
     * @param props      客户端配置项
     * @param clientName 可读名称，写入 client_name 字段
     */
    private void initPublicClientIfAbsent(OAuth2ClientProperties.ClientProperties props, String clientName) {
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
            .withId(props.getId())
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
        log.info("OAuth2 内置客户端 [{}] 初始化完成，id={}，redirect_uri={}",
                props.getClientId(), props.getId(), props.getRedirectUri());
    }

    /**
     * 若已存在客户端的 redirect_uri 与当前配置不一致，自动同步更新
     * <p>
     * 解决 nginx 统一入口改造后 backend 配置已更新但 DB 中留存旧 Vite 直连地址的问题
     *
     * @param props 当前配置中的客户端参数
     */
    private void syncRedirectUriIfChanged(OAuth2ClientProperties.ClientProperties props) {
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
     * 将已存在客户端的 access_token 格式升级为 REFERENCE（不透明令牌）
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
