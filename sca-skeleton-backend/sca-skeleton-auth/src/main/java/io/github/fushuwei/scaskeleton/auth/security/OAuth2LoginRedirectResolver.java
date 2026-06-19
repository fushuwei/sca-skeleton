package io.github.fushuwei.scaskeleton.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 表单登录成功后的重定向地址解析：从 Session pending authorize 取出 OAuth2 authorize URL。
 *
 * @author Fu Wei
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginRedirectResolver {

    /** 显式 pending authorize Session 存储（渠道感知，替代非渠道感知的 SavedRequest） */
    private final OAuth2PendingAuthorizeStore pendingAuthorizeStore;

    /**
     * 解析登录成功后浏览器应跳转的 authorize URL。
     *
     * @param request  当前请求
     * @param channel  当前渠道标识（admin / portal）
     * @param state    PKCE state，用于精确匹配该授权请求对应的 pending authorize
     * @return 经网关可访问的 authorize URL；无可用恢复目标时返回 null
     */
    public String resolvePostLoginRedirectUrl(HttpServletRequest request, String channel, String state) {
        if (!StringUtils.hasText(state)) {
            return null;
        }
        String pending = pendingAuthorizeStore.peekPendingAuthorizeUrl(request, channel, state);
        if (StringUtils.hasText(pending) && pending.contains("/oauth2/")) {
            return pending;
        }
        return null;
    }

    /**
     * 登录成功并完成跳转后清除对应（渠道 + state）的 pending authorize，避免重复消费。
     */
    public void removeSavedRequest(HttpServletRequest request, String channel, String state) {
        if (StringUtils.hasText(state)) {
            pendingAuthorizeStore.clearPendingAuthorizeUrl(request, channel, state);
        }
    }
}
