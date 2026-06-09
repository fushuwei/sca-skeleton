package io.github.fushuwei.scaskeleton.auth.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.net.URI;

/**
 * OAuth2 公共客户端配置属性类
 *
 * @author Fu Wei
 */
@Data
@ConfigurationProperties(prefix = "sca.auth")
public class OAuth2ClientProperties {

    /**
     * 认证中心对外暴露的请求路径前缀（经网关），与网关路由 predicates 配置保持一致
     */
    private String publicPathPrefix = "/auth";

    /**
     * 认证中心对外根 URL（经网关），作为 id_token 的 iss 声明，同时用于生成登录/授权重定向地址
     */
    private String issuer;

    /**
     * OAuth2 客户端配置
     */
    private Clients clients = new Clients();

    /**
     * 管理后台 SPA 客户端配置
     */
    public ClientProperties getAdmin() {
        return clients.getAdmin();
    }

    /**
     * 前台门户 SPA 客户端配置
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
         * 管理后台 SPA 客户端配置
         */
        private ClientProperties admin = new ClientProperties();

        /**
         * 前台门户 SPA 客户端配置
         */
        private ClientProperties portal = new ClientProperties();
    }

    /**
     * 单个 OAuth2 公共客户端配置项
     */
    @Data
    public static class ClientProperties {

        /**
         * OAuth2 客户端 ID
         */
        private String clientId;

        /**
         * OAuth2 授权码回调地址，用于前端 SPA 公共客户端接收授权码
         */
        private String redirectUri;

        /**
         * 访问令牌有效期（秒），默认 15 分钟
         */
        private long accessTokenTtl = 900;

        /**
         * 刷新令牌有效期（秒），默认 2 小时
         */
        private long refreshTokenTtl = 7200;
    }

    /**
     * 解析浏览器可访问的登录页地址（经 API 网关）
     *
     * @param clientId OAuth2 客户端 ID
     * @return 登录页地址
     */
    public String resolveExternalLoginUrl(String clientId) {
        return normalizeIssuer() + resolveLoginPath(clientId);
    }

    /**
     * 解析登录失败后的回跳地址（经 API 网关）
     *
     * @param clientId OAuth2 客户端 ID
     * @return 登录失败跳转地址
     */
    public String resolveExternalLoginFailureUrl(String clientId) {
        return resolveExternalLoginUrl(clientId) + "?error";
    }

    /**
     * 去掉 issuer 末尾斜杠，便于拼接路径
     */
    public String normalizeIssuer() {
        if (issuer == null || issuer.isBlank()) return "";
        return issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
    }

    /**
     * 根据客户端 ID 解析登录页的地址
     *
     * @param clientId 授权请求中的客户端 ID
     * @return 登录页的地址
     */
    private String resolveLoginPath(String clientId) {
        // 前台门户登录页的地址
        if (getPortal().getClientId() != null && getPortal().getClientId().equals(clientId)) {
            return "/login/portal";
        }

        // 默认返回管理后台登录页的地址
        return "/login/admin";
    }

    /**
     * 从 OAuth 回调地址中提取 SPA 公共客户端的根 URL
     * <p>
     * 通过 {@code /oauth/} 在路径中的位置来识别：
     * <ul>
     *   <li>admin：{@code /admin/oauth/callback} → 提取 {@code /admin/}</li>
     *   <li>portal：{@code /oauth/callback} → 提取 {@code /}</li>
     * </ul>
     *
     * @param redirectUri OAuth2 回调地址
     * @return SPA 公共客户端的根 URL
     */
    public String extractSpaRootUrl(String redirectUri) {
        if (!StringUtils.hasText(redirectUri)) {
            return null;
        }
        try {
            URI uri = URI.create(redirectUri);
            String path = uri.getPath();
            if (!StringUtils.hasText(path)) {
                return redirectUri;
            }
            String spaPath;
            if (path.indexOf("/oauth/") > 0) {
                spaPath = path.substring(0, path.indexOf("/oauth/"));
                if (!spaPath.endsWith("/")) {
                    spaPath = spaPath + "/";
                }
            } else {
                spaPath = "/";
            }
            String schemeAndHost = redirectUri.substring(0, redirectUri.indexOf(path));
            return schemeAndHost + spaPath;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
