package io.github.fushuwei.scaskeleton.auth.web;

import io.github.fushuwei.scaskeleton.auth.config.properties.OAuthClientsProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;

/**
 * 表单登录成功后的重定向地址解析：从 Session pending / SavedRequest 取出 OAuth2 authorize URL，并补齐经网关的 {@code /auth} 前缀。
 *
 * @author Fu Wei
 */
@Component
@RequiredArgsConstructor
public class OAuthLoginRedirectResolver {

    /** 与两条 SecurityFilterChain 共享的 SavedRequest 存储 */
    private final HttpSessionRequestCache requestCache;

    /** issuer / 对外路径前缀配置 */
    private final OAuthClientsProperties oauthClientsProperties;

    /** 显式 pending authorize Session 存储（优先于 SavedRequest） */
    private final OAuthPendingAuthorizeStore pendingAuthorizeStore;

    /**
     * 解析登录成功后浏览器应跳转的绝对 URL（通常为 {@code {issuer}/oauth2/authorize?...}）。
     *
     * @param request  当前请求
     * @param response 当前响应
     * @param channel  当前渠道标识（admin / portal），用于从渠道感知的 pending map 中精准取值
     * @return 经网关可访问的 authorize URL；无可用恢复目标时返回 null
     */
    public String resolvePostLoginRedirectUrl(HttpServletRequest request, HttpServletResponse response,
            String channel) {
        // 1) 优先使用 entry point 写入的 pending authorize（按渠道隔离，跨 Session 分裂场景更可靠）
        String pending = pendingAuthorizeStore.peekPendingAuthorizeUrl(request, channel);
        if (StringUtils.hasText(pending) && pending.contains("/oauth2/")) {
            return pending;
        }
        // 2) 回退 SavedRequest（同 Session 且未被覆盖时有效）
        SavedRequest saved = requestCache.getRequest(request, response);
        if (saved == null) {
            return null;
        }
        String normalized = normalizeAuthorizeRedirectUrl(saved.getRedirectUrl());
        if (!StringUtils.hasText(normalized) || !normalized.contains("/oauth2/")) {
            return null;
        }
        return normalized;
    }

    /**
     * 登录成功并完成跳转后清除 pending 与 SavedRequest，避免重复消费。
     *
     * @param channel 当前渠道标识（admin / portal），仅清除对应渠道的 pending
     */
    public void removeSavedRequest(HttpServletRequest request, HttpServletResponse response, String channel) {
        pendingAuthorizeStore.clearPendingAuthorizeUrl(request, channel);
        requestCache.removeRequest(request, response);
    }

    /**
     * 将 SavedRequest 中的 authorize URL 规范为浏览器经网关可访问的绝对地址（禁止返回内网 Auth 端口）。
     */
    private String normalizeAuthorizeRedirectUrl(String redirectUrl) {
        if (!StringUtils.hasText(redirectUrl)) {
            return null;
        }
        String issuer = normalizeIssuer();
        if (!StringUtils.hasText(issuer)) {
            return null;
        }
        // 已是正确对外 URL
        if (redirectUrl.startsWith(issuer + "/oauth2") || redirectUrl.startsWith(issuer + "/.well-known")) {
            return redirectUrl;
        }
        // 相对路径 /oauth2/authorize?...
        if (redirectUrl.startsWith("/oauth2")) {
            return issuer + redirectUrl;
        }
        try {
            URI uri = URI.create(redirectUrl);
            String path = uri.getPath();
            if (path == null || !path.contains("/oauth2/")) {
                return null;
            }
            String publicPrefix = oauthClientsProperties.getPublicPathPrefix();
            // 绝对 URL 已带 /auth 前缀且 host 与 issuer 一致则原样返回
            if (StringUtils.hasText(publicPrefix) && path.startsWith(publicPrefix + "/oauth2")
                    && redirectUrl.startsWith(issuer)) {
                return redirectUrl;
            }
            // 内网 URL（如 http://192.168.x.x:9001/oauth2/authorize）统一映射到 issuer
            int oauthIndex = path.indexOf("/oauth2/");
            String oauthPath = oauthIndex >= 0 ? path.substring(oauthIndex) : path;
            String query = uri.getRawQuery();
            return issuer + oauthPath + (StringUtils.hasText(query) ? "?" + query : "");
        } catch (IllegalArgumentException ex) {
            return null;
        }
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
