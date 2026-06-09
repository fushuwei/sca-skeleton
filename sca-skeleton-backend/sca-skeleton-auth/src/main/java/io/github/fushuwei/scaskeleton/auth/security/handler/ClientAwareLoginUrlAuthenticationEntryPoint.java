package io.github.fushuwei.scaskeleton.auth.security.handler;

import io.github.fushuwei.scaskeleton.auth.config.properties.OAuth2ClientProperties;
import io.github.fushuwei.scaskeleton.auth.security.OAuth2PendingAuthorizeStore;
import io.github.fushuwei.scaskeleton.auth.security.filter.AuthorizeChannelIsolationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.io.IOException;
import java.util.Map;

/**
 * 客户端感知的登录入口：根据 OAuth2 authorize 请求中的 {@code client_id} 跳转到不同登录页。
 * <p>
 * admin 客户端 → 网关 {@code /auth/login/admin}；portal 客户端 → 网关 {@code /auth/login/portal}。
 *
 * @author Fu Wei
 */
public class ClientAwareLoginUrlAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {

    /** OAuth2 客户端配置，用于 client_id → 登录页 URL 映射 */
    private final OAuth2ClientProperties oauth2ClientProperties;

    /** 显式保存待恢复的 authorize URL，避免 SavedRequest 被登录页覆盖或 Session 跨端口丢失 */
    private final OAuth2PendingAuthorizeStore pendingAuthorizeStore;

    /**
     * @param oauth2ClientProperties 客户端配置（admin / portal）
     * @param pendingAuthorizeStore  pending authorize Session 存储
     * @param defaultLoginUrl        未知 client_id 时的默认登录页绝对 URL
     */
    public ClientAwareLoginUrlAuthenticationEntryPoint(OAuth2ClientProperties oauth2ClientProperties,
                                                       OAuth2PendingAuthorizeStore pendingAuthorizeStore,
                                                       String defaultLoginUrl) {
        super(defaultLoginUrl);
        this.oauth2ClientProperties = oauth2ClientProperties;
        this.pendingAuthorizeStore = pendingAuthorizeStore;
    }

    /**
     * 未登录访问授权端点时，直接 302 到经网关暴露的绝对登录 URL。
     * <p>
     * 覆盖父类 {@code commence}，避免父类按 Auth 内网地址重写 Location。
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        // 若 AuthorizeChannelIsolationFilter 因渠道不匹配销毁了旧 Session，
        // 它会把旧 Session 的 pending authorize map 暂存到 request attribute。
        // 这里先恢复到新 Session，确保其他渠道的 pending 不丢失。
        restorePreservedPendingMap(request);
        // 在跳转登录页前写入当前渠道的 pending authorize，供表单登录成功后恢复（不依赖 SavedRequest 单槽位）
        pendingAuthorizeStore.savePendingAuthorizeRequest(request);
        // 从 authorize 请求 query 读取 client_id，映射 admin / portal 登录页
        String clientId = request.getParameter("client_id");
        String loginUrl = oauth2ClientProperties.resolveExternalLoginUrl(clientId);
        // 302 到网关登录页（浏览器后续请求仍走网关 /auth/**）
        response.sendRedirect(loginUrl);
    }

    /**
     * 将 {@link AuthorizeChannelIsolationFilter} 暂存的 pending authorize map 恢复到新 Session。
     */
    @SuppressWarnings("unchecked")
    private void restorePreservedPendingMap(HttpServletRequest request) {
        Object preserved = request.getAttribute(AuthorizeChannelIsolationFilter.PRESERVED_PENDING_MAP_ATTR);
        if (preserved instanceof Map) {
            pendingAuthorizeStore.writePendingMap(request, (Map<String, String>) preserved);
        }
    }
}
