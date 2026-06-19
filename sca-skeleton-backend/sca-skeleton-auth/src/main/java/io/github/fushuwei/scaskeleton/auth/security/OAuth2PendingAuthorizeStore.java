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
     * 以 PKCE state 为键存储，确保多 tab 同时授权时各自的 pending authorize 互不干扰。
     *
     * @param request 当前 authorize 请求（Auth 内路径 {@code /oauth2/authorize}）
     */
    public void savePendingAuthorizeRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String channel = resolveChannel(request);
        String state = request.getParameter("state");
        if (!StringUtils.hasText(state)) {
            return;
        }
        String key = channel + ":" + state;
        @SuppressWarnings("unchecked")
        Map<String, String> map = (Map<String, String>) session.getAttribute(SESSION_ATTRIBUTE);
        if (map == null) {
            map = new HashMap<>();
        }
        // 以 state 为键：同一渠道多 tab 各自有不同的 state，不会互相覆盖
        if (map.containsKey(key)) {
            return;
        }
        String url = buildExternalAuthorizeUrl(request);
        map.put(key, url);
        session.setAttribute(SESSION_ATTRIBUTE, map);
    }

    /**
     * 读取并清除 Session 中指定（渠道 + state）的 pending authorize URL（一次性消费）。
     *
     * @param request 当前请求
     * @param channel 渠道标识（admin / portal）
     * @param state   PKCE state，用于精确匹配
     * @return 经网关的 authorize 绝对 URL；不存在时返回 null
     */
    public String consumePendingAuthorizeUrl(HttpServletRequest request, String channel, String state) {
        return removePending(request, channel, state);
    }

    /**
     * 读取 Session 中指定（渠道 + state）的 pending authorize URL（不清除）。
     *
     * @param request 当前请求
     * @param channel 渠道标识（admin / portal）
     * @param state   PKCE state，用于精确匹配
     */
    public String peekPendingAuthorizeUrl(HttpServletRequest request, String channel, String state) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, String> map = (Map<String, String>) session.getAttribute(SESSION_ATTRIBUTE);
        if (map == null) {
            return null;
        }
        String key = channel + ":" + state;
        String url = map.get(key);
        return StringUtils.hasText(url) ? url : null;
    }

    /** 清除指定（渠道 + state）的 pending authorize URL。 */
    public void clearPendingAuthorizeUrl(HttpServletRequest request, String channel, String state) {
        removePending(request, channel, state);
    }

    /** 读取并删除 pending authorize。 */
    private String removePending(HttpServletRequest request, String channel, String state) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, String> map = (Map<String, String>) session.getAttribute(SESSION_ATTRIBUTE);
        if (map == null) {
            return null;
        }
        String key = channel + ":" + state;
        String url = map.remove(key);
        if (map.isEmpty()) {
            session.removeAttribute(SESSION_ATTRIBUTE);
        } else {
            session.setAttribute(SESSION_ATTRIBUTE, map);
        }
        return StringUtils.hasText(url) ? url : null;
    }

    /**
     * 判断当前 Session 中是否存在指定渠道的 pending authorize（不清除）。
     * <p>
     * 用于 LoginPageController 判断登录页是否经 OAuth2 流程跳转而来。
     */
    public boolean hasPendingForChannel(HttpServletRequest request, String channel) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Map<String, String> map = (Map<String, String>) session.getAttribute(SESSION_ATTRIBUTE);
        if (map == null) {
            return false;
        }
        String prefix = channel + ":";
        for (String key : map.keySet()) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
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
