package io.github.fushuwei.scaskeleton.auth.security;

import io.github.fushuwei.scaskeleton.auth.config.properties.OAuth2ClientProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 在 Session 中显式保存待恢复的 OAuth2 {@code /oauth2/authorize} 绝对 URL。
 * <p>
 * 以渠道（admin / portal）为键分别存储，避免同一浏览器多标签同时授权时互相覆盖。
 * <p>
 * 解决浏览器经 Vite 代理访问 authorize 与经网关访问登录页时 Session 不一致、
 * {@link org.springframework.security.web.savedrequest.HttpSessionRequestCache} 被登录页覆盖的问题。
 *
 * @author Fu Wei
 */
@Component
@RequiredArgsConstructor
public class OAuth2PendingAuthorizeStore {

    /** Session 键：待恢复的 authorize 绝对 URL Map（渠道 → 经网关 URL） */
    static final String SESSION_ATTRIBUTE = "SCA_OAUTH2_PENDING_AUTHORIZE_MAP";

    /** issuer / 对外路径前缀配置 */
    private final OAuth2ClientProperties oauth2ClientProperties;

    /**
     * 未登录访问 {@code /oauth2/authorize} 并重定向到登录页之前，保存经网关可访问的 authorize URL。
     * <p>
     * 按 {@code client_id} 对应的渠道分别存储，admin 与 portal 互不干扰。
     *
     * @param request 当前 authorize 请求（Auth 内路径 {@code /oauth2/authorize}）
     */
    public void savePendingAuthorizeRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String channel = resolveChannel(request);
        @SuppressWarnings("unchecked")
        Map<String, String> map = (Map<String, String>) session.getAttribute(SESSION_ATTRIBUTE);
        if (map == null) {
            map = new HashMap<>();
        }
        // 同一渠道已有 pending 时不再覆盖：多 tab 同时 PKCE 会产生不同的 state，
        // 覆盖会导致先到的 tab 登录后 state 校验失败。
        if (map.containsKey(channel)) {
            return;
        }
        String url = buildExternalAuthorizeUrl(request);
        map.put(channel, url);
        session.setAttribute(SESSION_ATTRIBUTE, map);
    }

    /**
     * 读取并清除 Session 中指定渠道的 pending authorize URL（一次性消费）。
     *
     * @param request 当前请求
     * @param channel 渠道标识（admin / portal）
     * @return 经网关的 authorize 绝对 URL；不存在时返回 null
     */
    public String consumePendingAuthorizeUrl(HttpServletRequest request, String channel) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, String> map = (Map<String, String>) session.getAttribute(SESSION_ATTRIBUTE);
        if (map == null) {
            return null;
        }
        String url = map.remove(channel);
        if (map.isEmpty()) {
            session.removeAttribute(SESSION_ATTRIBUTE);
        } else {
            session.setAttribute(SESSION_ATTRIBUTE, map);
        }
        return StringUtils.hasText(url) ? url : null;
    }

    /**
     * 读取 Session 中指定渠道的 pending authorize URL（不清除）。
     *
     * @param request 当前请求
     * @param channel 渠道标识（admin / portal）
     */
    public String peekPendingAuthorizeUrl(HttpServletRequest request, String channel) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, String> map = (Map<String, String>) session.getAttribute(SESSION_ATTRIBUTE);
        if (map == null) {
            return null;
        }
        String url = map.get(channel);
        return StringUtils.hasText(url) ? url : null;
    }

    /** 清除指定渠道的 pending authorize URL。 */
    public void clearPendingAuthorizeUrl(HttpServletRequest request, String channel) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, String> map = (Map<String, String>) session.getAttribute(SESSION_ATTRIBUTE);
        if (map == null) {
            return;
        }
        map.remove(channel);
        if (map.isEmpty()) {
            session.removeAttribute(SESSION_ATTRIBUTE);
        } else {
            session.setAttribute(SESSION_ATTRIBUTE, map);
        }
    }

    // ── 跨 Session 迁移方法（渠道不匹配时 session.invalidate 前后使用） ──

    /**
     * 读取整个 pending authorize map（用于 session.invalidate 前保存）。
     *
     * @return pending map 的副本；不存在时返回 null
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> readPendingMap(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object raw = session.getAttribute(SESSION_ATTRIBUTE);
        if (!(raw instanceof Map)) {
            return null;
        }
        return new HashMap<>((Map<String, String>) raw);
    }

    /**
     * 将 pending authorize map 写入 Session（用于新 Session 创建后恢复）。
     *
     * @param pendingMap 待恢复的 pending map；null 或空则忽略
     */
    public void writePendingMap(HttpServletRequest request, Map<String, String> pendingMap) {
        if (pendingMap == null || pendingMap.isEmpty()) {
            return;
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_ATTRIBUTE, new HashMap<>(pendingMap));
    }

    /**
     * 从 authorize 请求的 {@code client_id} 参数解析对应渠道。
     */
    private String resolveChannel(HttpServletRequest request) {
        String clientId = request.getParameter("client_id");
        if (StringUtils.hasText(clientId)
                && clientId.equals(oauth2ClientProperties.getPortal().getClientId())) {
            return LoginChannel.PORTAL.getValue();
        }
        return LoginChannel.ADMIN.getValue();
    }

    /**
     * 将当前 authorize 请求拼成浏览器经网关访问的绝对 URL。
     */
    private String buildExternalAuthorizeUrl(HttpServletRequest request) {
        String issuer = oauth2ClientProperties.normalizeIssuer();
        String query = request.getQueryString();
        String authorizePath = "/oauth2/authorize";
        if (!StringUtils.hasText(issuer)) {
            return authorizePath + (StringUtils.hasText(query) ? "?" + query : "");
        }
        return issuer + authorizePath + (StringUtils.hasText(query) ? "?" + query : "");
    }
}
