package io.github.fushuwei.scaskeleton.auth.web;

import io.github.fushuwei.scaskeleton.auth.config.properties.OAuthClientsProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 在 Session 中显式保存待恢复的 OAuth2 {@code /oauth2/authorize} 绝对 URL。
 * <p>
 * 解决浏览器经 Vite 代理访问 authorize 与经网关访问登录页时 Session 不一致、
 * {@link org.springframework.security.web.savedrequest.HttpSessionRequestCache} 被登录页覆盖的问题。
 *
 * @author Fu Wei
 */
@Component
@RequiredArgsConstructor
public class OAuthPendingAuthorizeStore {

    /** Session 键：待恢复的 authorize 绝对 URL（经网关） */
    static final String SESSION_ATTRIBUTE = "SCA_OAUTH2_PENDING_AUTHORIZE_URL";

    /** issuer / 对外路径前缀配置 */
    private final OAuthClientsProperties oauthClientsProperties;

    /**
     * 未登录访问 {@code /oauth2/authorize} 并重定向到登录页之前，保存经网关可访问的 authorize URL。
     *
     * @param request 当前 authorize 请求（Auth 内路径 {@code /oauth2/authorize}）
     */
    public void savePendingAuthorizeRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_ATTRIBUTE, buildExternalAuthorizeUrl(request));
    }

    /**
     * 读取并清除 Session 中的 pending authorize URL（一次性消费）。
     *
     * @param request 当前请求
     * @return 经网关的 authorize 绝对 URL；不存在时返回 null
     */
    public String consumePendingAuthorizeUrl(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_ATTRIBUTE);
        session.removeAttribute(SESSION_ATTRIBUTE);
        if (!(value instanceof String url) || !StringUtils.hasText(url)) {
            return null;
        }
        return url;
    }

    /**
     * 读取 Session 中的 pending authorize URL（不清除）。
     */
    public String peekPendingAuthorizeUrl(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_ATTRIBUTE);
        return value instanceof String url && StringUtils.hasText(url) ? url : null;
    }

    /** 清除 pending authorize URL。 */
    public void clearPendingAuthorizeUrl(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SESSION_ATTRIBUTE);
        }
    }

    /**
     * 将当前 authorize 请求拼成浏览器经网关访问的绝对 URL。
     */
    private String buildExternalAuthorizeUrl(HttpServletRequest request) {
        String issuer = normalizeIssuer();
        String query = request.getQueryString();
        String authorizePath = "/oauth2/authorize";
        if (!StringUtils.hasText(issuer)) {
            return authorizePath + (StringUtils.hasText(query) ? "?" + query : "");
        }
        return issuer + authorizePath + (StringUtils.hasText(query) ? "?" + query : "");
    }

    /** 去掉 issuer 末尾斜杠。 */
    private String normalizeIssuer() {
        String issuer = oauthClientsProperties.getIssuer();
        if (!StringUtils.hasText(issuer)) {
            return "";
        }
        return issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
    }
}
