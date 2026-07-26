package io.github.fushuwei.scaskeleton.auth.initializer;

import io.github.fushuwei.scaskeleton.auth.config.properties.OAuth2ClientProperties;
import io.github.fushuwei.scaskeleton.auth.grant.OAuth2GrantTypeConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * OAuth2 机密客户端初始化程序
 * <p>
 * 密码模式下，admin 和 portal 均为机密客户端（携带 client_secret），
 * 通过 {@code client_secret_basic} 方式认证，支持 {@code password} 和 {@code refresh_token} 授权类型。
 *
 * @author Fu Wei
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2RegisteredClientInitializer implements ApplicationRunner {

    /**
     * JDBC + Redis 缓存的客户端存储库
     */
    private final RegisteredClientRepository registeredClientRepository;

    /**
     * 公共客户端配置属性
     */
    private final OAuth2ClientProperties oauth2ClientProperties;

    /**
     * JDBC 模板（用于清理不兼容的旧记录）
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 密码编码器（用于加密 client_secret）
     */
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        // 清理 DB 中由旧 SQL 安装脚本写入的格式不兼容记录
        repairCorruptedClientIfNeeded(oauth2ClientProperties.getAdmin().getClientId());
        repairCorruptedClientIfNeeded(oauth2ClientProperties.getPortal().getClientId());
        // 初始化管理后台机密客户端
        initConfidentialClientIfAbsent(oauth2ClientProperties.getAdmin(), "SCA Admin SPA");
        // 初始化前台门户机密客户端
        initConfidentialClientIfAbsent(oauth2ClientProperties.getPortal(), "SCA Portal SPA");
        // 将历史公共客户端迁移为机密客户端（授权码模式 → 密码模式）
        migratePublicClientIfNeeded(oauth2ClientProperties.getAdmin());
        migratePublicClientIfNeeded(oauth2ClientProperties.getPortal());
    }

    /**
     * 若 DB 中的客户端记录因 JSON 格式不兼容导致反序列化失败，先删除再让后续逻辑重建。
     */
    private void repairCorruptedClientIfNeeded(String clientId) {
        if (clientId == null) {
            return;
        }
        try {
            registeredClientRepository.findByClientId(clientId);
        } catch (Exception e) {
            log.warn("OAuth2 客户端 [{}] 记录格式不兼容，将在 DB 中删除后重建：{}", clientId, e.getMessage());
            jdbcTemplate.update("DELETE FROM oauth2_registered_client WHERE client_id = ?", clientId);
        }
    }

    /**
     * 若 client_id 不存在则注册机密客户端（携带 client_secret，支持密码模式 + 刷新令牌）。
     */
    private void initConfidentialClientIfAbsent(OAuth2ClientProperties.ClientProperties props, String clientName) {
        if (props.getClientId() == null || props.getClientSecret() == null) {
            log.warn("OAuth2 客户端配置不完整，跳过初始化：clientId={}, clientSecret={}",
                    props.getClientId(), props.getClientSecret() != null ? "已配置" : "缺失");
            return;
        }
        if (registeredClientRepository.findByClientId(props.getClientId()) != null) {
            log.info("OAuth2 客户端 [{}] 已存在，跳过初始化", props.getClientId());
            return;
        }
        RegisteredClient client = buildConfidentialClient(props, clientName);
        registeredClientRepository.save(client);
        log.info("OAuth2 机密客户端 [{}] 初始化完成，id={}",
                props.getClientId(), props.getId());
    }

    /**
     * 若已存在客户端仍是公共客户端（无 client_secret 或 grant_type 含 authorization_code），
     * 迁移为机密客户端（添加 client_secret，替换授权类型为 password + refresh_token）。
     */
    private void migratePublicClientIfNeeded(OAuth2ClientProperties.ClientProperties props) {
        if (props.getClientId() == null || props.getClientSecret() == null) {
            return;
        }
        RegisteredClient existing = registeredClientRepository.findByClientId(props.getClientId());
        if (existing == null) {
            return;
        }
        // 判断是否需要迁移：公共客户端（无 secret）或授权类型含 authorization_code
        boolean isPublicClient = existing.getClientSecret() == null;
        boolean hasAuthorizationCode = existing.getAuthorizationGrantTypes().stream()
                .anyMatch(AuthorizationGrantType.AUTHORIZATION_CODE::equals);
        if (!isPublicClient && !hasAuthorizationCode) {
            // 已是机密客户端且无 authorization_code，检查 token 格式
            migrateToOpaqueAccessTokenIfNeeded(existing, props);
            return;
        }
        log.warn("OAuth2 客户端 [{}] 需要从公共客户端迁移为机密客户端（密码模式）", props.getClientId());
        RegisteredClient migrated = buildConfidentialClient(props, existing.getClientName());
        registeredClientRepository.save(migrated);
        log.info("OAuth2 客户端 [{}] 已迁移为机密客户端（密码模式）", props.getClientId());
    }

    /**
     * 将已存在客户端的 access_token 格式升级为 REFERENCE（不透明令牌）。
     */
    private void migrateToOpaqueAccessTokenIfNeeded(RegisteredClient existing,
                                                     OAuth2ClientProperties.ClientProperties props) {
        if (OAuth2TokenFormat.REFERENCE.equals(existing.getTokenSettings().getAccessTokenFormat())) {
            return;
        }
        log.warn("OAuth2 客户端 [{}] 正在迁移为不透明访问令牌（REFERENCE）", props.getClientId());
        RegisteredClient updated = RegisteredClient.from(existing)
                .tokenSettings(TokenSettings.builder()
                        .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                        .accessTokenTimeToLive(Duration.ofSeconds(props.getAccessTokenTtl()))
                        .refreshTokenTimeToLive(Duration.ofSeconds(props.getRefreshTokenTtl()))
                        .reuseRefreshTokens(false)
                        .build())
                .build();
        registeredClientRepository.save(updated);
        log.info("OAuth2 客户端 [{}] 已升级为不透明访问令牌", props.getClientId());
    }

    /**
     * 构建机密客户端：密码模式 + 刷新令牌 + 不透明令牌。
     */
    private RegisteredClient buildConfidentialClient(OAuth2ClientProperties.ClientProperties props, String clientName) {
        return RegisteredClient
                .withId(props.getId())
                .clientId(props.getClientId())
                .clientSecret(passwordEncoder.encode(props.getClientSecret()))
                .clientName(clientName)
                // 机密客户端：通过 client_secret_basic 方式认证
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                // 密码模式 + 刷新令牌
                .authorizationGrantType(OAuth2GrantTypeConstants.PASSWORD)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .scope("profile")
                .scope("all")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                        .accessTokenTimeToLive(Duration.ofSeconds(props.getAccessTokenTtl()))
                        .refreshTokenTimeToLive(Duration.ofSeconds(props.getRefreshTokenTtl()))
                        .reuseRefreshTokens(false)
                        .build())
                .build();
    }
}
