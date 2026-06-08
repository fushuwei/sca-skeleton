package io.github.fushuwei.scaskeleton.auth.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * OAuth2 公共客户端（admin / portal）及 Auth 对外路径配置。
 * <p>
 * 通过 {@code sca.auth.*} 外部化配置，由 {@link io.github.fushuwei.scaskeleton.auth.config.RegisteredClientInitializer}
 * 在本地/测试环境首次启动时写入 {@code oauth2_registered_client}。
 *
 * @author Fu Wei
 */
@Validated
@Data
@ConfigurationProperties(prefix = "sca.auth")
public class OAuthClientsProperties {

    /**
     * 经 API 网关对外暴露的路径前缀（开发环境默认 {@code /auth}）。
     * <p>
     * 浏览器重定向与登录表单 action 必须带此前缀；Auth 服务内部仍使用 {@code /login/**}。
     */
    private String publicPathPrefix = "/auth";

    /**
     * OAuth2 issuer（与 {@code AuthorizationServerSettings} 一致）。
     * <p>
     * 由 {@code sca.auth.issuer} / 环境变量 {@code AUTH_ISSUER} 注入，必须在 {@code sca-skeleton-backend/.env} 中配置。
     * 用于生成浏览器可见的绝对登录 URL，避免重定向到 Auth 内网端口。
     */
    @NotBlank(message = "必须在 sca-skeleton-backend/.env 中配置 AUTH_ISSUER（API 网关对外 /auth 根 URL，例如 http://localhost:9999/auth）")
    private String issuer;

    /** admin / portal 客户端注册参数 */
    private Clients clients = new Clients();

    /** 管理后台 SPA 客户端配置 */
    public ClientProperties getAdmin() {
        return clients.getAdmin();
    }

    /** 前台门户 SPA 客户端配置 */
    public ClientProperties getPortal() {
        return clients.getPortal();
    }

    /**
     * OAuth2 客户端分组配置（对应 {@code sca.auth.clients.*}）。
     */
    @Data
    public static class Clients {

        /** 管理后台 SPA */
        private ClientProperties admin = new ClientProperties();

        /** 前台门户 SPA */
        private ClientProperties portal = new ClientProperties();
    }

    /**
     * 单个 OAuth2 公共客户端配置项。
     */
    @Data
    public static class ClientProperties {

        /** OAuth2 client_id，对外暴露且可写进前端环境变量 */
        private String clientId;

        /** PKCE 授权码回调地址，必须与 SPA 路由 {@code /oauth/callback} 完全一致 */
        private String redirectUri;

        /** 访问令牌有效期（秒） */
        private long accessTokenTtl = 900;

        /** 刷新令牌有效期（秒） */
        private long refreshTokenTtl = 7200;
    }

    /**
     * 根据 client_id 解析登录页路径（admin / portal 使用不同 UI）。
     *
     * @param clientId 授权请求中的 client_id
     * @return 登录页路径；未知 client 时回退 admin 登录页
     */
    public String resolveLoginPath(String clientId) {
        // portal 客户端命中时返回门户登录页
        if (getPortal().getClientId() != null && getPortal().getClientId().equals(clientId)) {
            return "/login/portal";
        }
        // 默认走管理后台登录页（含 admin client 与未知 client 兜底）
        return "/login/admin";
    }

    /**
     * 根据 client_id 解析登录失败后的回跳路径。
     *
     * @param clientId 表单提交的 loginChannel 或 authorize 请求中的 client_id
     * @return 带 {@code error} 查询参数的登录页路径
     */
    public String resolveLoginFailurePath(String clientId) {
        // 与 resolveLoginPath 保持一致，仅在 URL 上附加 error 标记
        return resolveLoginPath(clientId) + "?error";
    }

    /**
     * 将 Auth 服务内部路径转换为浏览器经网关访问的路径。
     *
     * @param internalPath 服务内路径，如 {@code /login/admin}
     * @return 对外路径，如 {@code /auth/login/admin}
     */
    public String toExternalPath(String internalPath) {
        // 空路径仅返回前缀
        if (internalPath == null || internalPath.isEmpty()) {
            return publicPathPrefix;
        }
        // 已带前缀则不再重复拼接
        if (internalPath.startsWith(publicPathPrefix)) {
            return internalPath;
        }
        return publicPathPrefix + internalPath;
    }

    /**
     * 解析经网关对外暴露的登录页 URL（相对路径，如 {@code /auth/login/portal}）。
     *
     * @param clientId OAuth2 client_id；可为 null（回退 admin）
     * @return 如 {@code /auth/login/portal}
     */
    public String resolveExternalLoginPath(String clientId) {
        return toExternalPath(resolveLoginPath(clientId));
    }

    /**
     * 解析浏览器可见的绝对登录页 URL（经 API 网关）。
     *
     * @param clientId OAuth2 client_id；可为 null（回退 admin）
     * @return 如 {@code {issuer}/login/portal}
     */
    public String resolveExternalLoginUrl(String clientId) {
        return normalizeIssuer() + resolveLoginPath(clientId);
    }

    /**
     * 解析登录失败后的对外回跳 URL（相对路径）。
     *
     * @param clientId OAuth2 client_id 或渠道映射后的 clientId
     * @return 如 {@code /auth/login/admin?error}
     */
    public String resolveExternalLoginFailurePath(String clientId) {
        return resolveExternalLoginPath(clientId) + "?error";
    }

    /**
     * 解析登录失败后的绝对回跳 URL（经 API 网关）。
     *
     * @param clientId OAuth2 client_id 或渠道映射后的 clientId
     * @return 如 {@code {issuer}/login/admin?error}
     */
    public String resolveExternalLoginFailureUrl(String clientId) {
        return resolveExternalLoginUrl(clientId) + "?error";
    }

    /** 登录表单 POST 的绝对 URL（浏览器经网关提交）。 */
    public String getExternalLoginProcessingUrl() {
        return normalizeIssuer() + "/login/authenticate";
    }

    /** 去掉 issuer 末尾斜杠，便于拼接路径。 */
    private String normalizeIssuer() {
        if (issuer == null || issuer.isBlank()) {
            return "";
        }
        return issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
    }

    /**
     * 从 OAuth redirect_uri 提取 SPA 根 URL（如 {@code http://localhost:8080/admin/oauth/callback} → {@code http://localhost:8080/admin/}）。
     * <p>
     * 通过在 path 中定位 {@code /oauth/} 来区分不同 base path 的 SPA：
     * <ul>
     *   <li>admin：{@code /admin/oauth/callback} → 提取 {@code /admin/}</li>
     *   <li>portal：{@code /oauth/callback} → 提取 {@code /}</li>
     * </ul>
     *
     * @param redirectUri OAuth2 回调地址
     * @return SPA 根 URL；无法解析时返回 null
     */
    public String extractSpaRootUrl(String redirectUri) {
        if (!org.springframework.util.StringUtils.hasText(redirectUri)) {
            return null;
        }
        try {
            java.net.URI uri = java.net.URI.create(redirectUri);
            String path = uri.getPath();
            if (!org.springframework.util.StringUtils.hasText(path)) {
                return redirectUri;
            }
            // 通过 /oauth/ 在路径中的位置来识别 SPA 的 base path
            int oauthIdx = path.indexOf("/oauth/");
            String spaPath;
            if (oauthIdx > 0) {
                // 有 base path（如 /admin）：保留 /oauth/ 之前的部分
                spaPath = path.substring(0, oauthIdx);
                if (!spaPath.endsWith("/")) {
                    spaPath = spaPath + "/";
                }
            } else {
                // 无 base path（如 /oauth/callback）或未找到 /oauth/
                spaPath = "/";
            }
            String schemeAndHost = redirectUri.substring(0, redirectUri.indexOf(path));
            return schemeAndHost + spaPath;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
