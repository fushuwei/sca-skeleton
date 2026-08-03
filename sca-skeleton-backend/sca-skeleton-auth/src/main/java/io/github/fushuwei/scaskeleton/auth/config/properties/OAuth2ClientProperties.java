package io.github.fushuwei.scaskeleton.auth.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth2 机密客户端配置属性类
 * <p>
 * 密码模式下，admin 和 portal 均为机密客户端（携带 client_secret），
 * 通过 {@code client_secret_basic} 方式认证。不再需要 redirect_uris 和
 * 授权码流程相关的 URL 解析方法。
 *
 * @author Fu Wei
 */
@Data
@ConfigurationProperties(prefix = "sca.auth")
public class OAuth2ClientProperties {

    /**
     * OAuth2 客户端配置
     */
    private Clients clients = new Clients();

    /**
     * 管理后台客户端配置
     */
    public ClientProperties getAdmin() {
        return clients.getAdmin();
    }

    /**
     * 前台门户客户端配置
     */
    public ClientProperties getPortal() {
        return clients.getPortal();
    }

    /**
     * OAuth2 客户端配置
     */
    @Data
    public static class Clients {

        /**
         * 管理后台客户端配置
         */
        private ClientProperties admin = new ClientProperties();

        /**
         * 前台门户客户端配置
         */
        private ClientProperties portal = new ClientProperties();
    }

    /**
     * 单个 OAuth2 机密客户端配置项
     */
    @Data
    public static class ClientProperties {

        /**
         * OAuth2 客户端主键 ID
         */
        private String id;

        /**
         * OAuth2 客户端 ID
         */
        private String clientId;

        /**
         * OAuth2 客户端密钥（明文配置，初始化时由 PasswordEncoder 加密后写入 DB）
         * <p>
         * 生产环境应通过环境变量注入，不硬编码到 YAML：
         * {@code AUTH_ADMIN_CLIENT_SECRET} / {@code AUTH_PORTAL_CLIENT_SECRET}
         */
        private String clientSecret;

        /**
         * 访问令牌有效期（秒），默认 15 分钟
         */
        private long accessTokenTtl = 900;

        /**
         * 刷新令牌有效期（秒），默认 2 小时
         */
        private long refreshTokenTtl = 7200;
    }
}
