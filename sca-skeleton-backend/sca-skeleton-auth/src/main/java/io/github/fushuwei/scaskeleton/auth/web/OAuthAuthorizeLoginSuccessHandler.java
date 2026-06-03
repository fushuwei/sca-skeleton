package io.github.fushuwei.scaskeleton.auth.web;

import io.github.fushuwei.scaskeleton.auth.config.properties.OAuthClientsProperties;
import io.github.fushuwei.scaskeleton.auth.security.LoginChannel;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * 表单登录成功处理器：恢复 OAuth2 {@code /oauth2/authorize} pending / SavedRequest，并强制跳转到经网关的绝对 URL。
 *
 * @author Fu Wei
 */
@Component
@RequiredArgsConstructor
public class OAuthAuthorizeLoginSuccessHandler implements AuthenticationSuccessHandler {

    /** 解析并规范化 authorize 回跳 URL */
    private final OAuthLoginRedirectResolver redirectResolver;

    /** OAuth2 客户端与 issuer 配置 */
    private final OAuthClientsProperties oauthClientsProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        // 登录成功后将渠道写入 Session，后续 authorize 请求据此做 admin/portal 会话隔离。
        HttpSession session = request.getSession(true);
        String loginChannel = request.getParameter("loginChannel");
        session.setAttribute(AuthSessionAttributes.LOGIN_CHANNEL,
                LoginChannel.fromValue(loginChannel).getValue());
        String target = redirectResolver.resolvePostLoginRedirectUrl(request, response);
        if (StringUtils.hasText(target)) {
            redirectResolver.removeSavedRequest(request, response);
            response.sendRedirect(target);
            return;
        }
        // 无 OAuth 恢复目标（如退出后直接访问登录页，或 SavedRequest 超时过期）：
        // 跳转到 SPA 根路径，由 SPA 路由守卫检测无本地 token 后自动发起 PKCE → authorize 流程。
        // 此时 Auth 服务已有有效 Session，authorize 请求将直接通过，回调 SPA 完成令牌交换。
        String clientId = LoginChannel.PORTAL.getValue().equals(loginChannel)
                ? oauthClientsProperties.getPortal().getClientId()
                : oauthClientsProperties.getAdmin().getClientId();
        String redirectUri = LoginChannel.PORTAL.getValue().equals(loginChannel)
                ? oauthClientsProperties.getPortal().getRedirectUri()
                : oauthClientsProperties.getAdmin().getRedirectUri();
        // 从 redirect_uri 提取 SPA 根路径（如 http://localhost:5173/oauth/callback → http://localhost:5173/）
        String spaRoot = extractSpaRoot(redirectUri);
        if (StringUtils.hasText(spaRoot)) {
            response.sendRedirect(spaRoot);
        } else {
            // 兜底：跳转到对应登录页（此分支仅在 redirectUri 配置异常时触发）
            response.sendRedirect(oauthClientsProperties.resolveExternalLoginUrl(clientId));
        }
    }

    /** 从 OAuth redirect_uri 提取 SPA 根路径（如 {@code http://localhost:5173/oauth/callback} → {@code http://localhost:5173/}）。 */
    private String extractSpaRoot(String redirectUri) {
        if (!StringUtils.hasText(redirectUri)) {
            return null;
        }
        try {
            java.net.URI uri = java.net.URI.create(redirectUri);
            String path = uri.getPath();
            if (StringUtils.hasText(path) && !"/".equals(path)) {
                // 去掉回调路径部分，保留 SPA 根路径
                String root = redirectUri.substring(0, redirectUri.indexOf(path)) + "/";
                return root.endsWith("//") ? root.substring(0, root.length() - 1) : root;
            }
            return redirectUri;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
