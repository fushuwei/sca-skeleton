package io.github.fushuwei.scaskeleton.auth.web;

import io.github.fushuwei.scaskeleton.auth.config.properties.OAuthClientsProperties;
import io.github.fushuwei.scaskeleton.auth.security.LoginChannel;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        String target = redirectResolver.resolvePostLoginRedirectUrl(request, response);
        if (StringUtils.hasText(target)) {
            redirectResolver.removeSavedRequest(request, response);
            response.sendRedirect(target);
            return;
        }
        // 无 OAuth 恢复目标时回到登录页并提示（禁止回退 SavedRequestAwareAuthenticationSuccessHandler 以免跳内网地址）
        String loginChannel = request.getParameter("loginChannel");
        String clientId = LoginChannel.PORTAL.getValue().equals(loginChannel)
                ? oauthClientsProperties.getPortal().getClientId()
                : oauthClientsProperties.getAdmin().getClientId();
        response.sendRedirect(oauthClientsProperties.resolveExternalLoginFailureUrl(clientId) + "&reason=oauth_session");
    }
}
