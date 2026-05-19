package io.github.fushuwei.scaskeleton.auth.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth2 默认 Web 客户端注册属性
 * <p>
 * 通过 {@code sca.auth.client.web.*} 在 {@code application.yml} 中配置，
 * 由 {@code RegisteredClientInitializer} 在服务启动时读取以创建或迁移默认客户端。
 * <p>
 * 安全注意：{@code clientSecret} 默认值带 {@code {noop}} 前缀仅适合本地开发环境，
 * 生产环境必须通过环境变量或配置中心注入 {@code {bcrypt}} 前缀的密文。
 *
 * @author Fu Wei
 */
@Data
@ConfigurationProperties(prefix = "sca.auth.client.web")
public class WebClientProperties {

    /**
     * OAuth2 客户端 ID（client_id），默认 {@code sca-web-client}
     */
    private String clientId = "sca-web-client";

    /**
     * 客户端密钥；本地开发使用 {@code {noop}} 明文前缀，生产环境必须替换为 {@code {bcrypt}} 密文
     */
    private String clientSecret = "{noop}sca-web-secret";

    /**
     * 访问令牌有效期（秒），默认 7200（2 小时）
     */
    private long accessTokenTtl = 7200;

    /**
     * 刷新令牌有效期（秒），默认 604800（7 天）
     */
    private long refreshTokenTtl = 604800;
}
