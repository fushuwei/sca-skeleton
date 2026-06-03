package io.github.fushuwei.scaskeleton.auth.security.filter;

import io.github.fushuwei.scaskeleton.auth.config.properties.OAuthClientsProperties;
import io.github.fushuwei.scaskeleton.auth.security.LoginChannel;
import io.github.fushuwei.scaskeleton.auth.web.AuthSessionAttributes;
import io.github.fushuwei.scaskeleton.auth.web.OAuthPendingAuthorizeStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * 授权端点渠道隔离过滤器：阻断 admin 与 portal 共用同一 Auth 登录会话。
 * <p>
 * 当浏览器已登录 admin 会话却请求 portal 的 {@code /oauth2/authorize} 时，过滤器会主动清空当前会话，
 * 强制回到 portal 登录页，避免“管理员无感登录前台”风险。
 *
 * @author Fu Wei
 */
@Component
@RequiredArgsConstructor
public class AuthorizeChannelIsolationFilter extends OncePerRequestFilter {

    /** OAuth2 客户端配置（用于 client_id -> channel 映射）。 */
    private final OAuthClientsProperties oauthClientsProperties;

    /** Pending authorize 存储（用于 session.invalidate 前保存渠道级 pending 数据）。 */
    private final OAuthPendingAuthorizeStore pendingAuthorizeStore;

    /** Request attribute：暂存从旧 Session 迁出的 pending authorize map，供 EntryPoint 恢复到新 Session。 */
    public static final String PRESERVED_PENDING_MAP_ATTR = "SCA_PRESERVED_PENDING_AUTHORIZE_MAP";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        // 仅处理授权端点，其余请求直接放行。
        if (!isAuthorizeEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        // 当前请求未登录时不处理，交给默认认证入口跳转登录页。
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            filterChain.doFilter(request, response);
            return;
        }
        // 已登录但 session 记录渠道与 authorize client_id 不一致时，
        // 销毁旧 Session（刷新 CSRF token），但通过 request attribute 保留 pending authorize map，
        // 供 ClientAwareLoginUrlAuthenticationEntryPoint 恢复到新 Session。
        if (!isChannelMatched(request)) {
            Map<String, String> preservedPendingMap = pendingAuthorizeStore.readPendingMap(request);
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();
            // 暂存到 request attribute，后续 EntryPoint 的 savePendingAuthorizeRequest 会合并到新 Session
            if (preservedPendingMap != null && !preservedPendingMap.isEmpty()) {
                request.setAttribute(PRESERVED_PENDING_MAP_ATTR, preservedPendingMap);
            }
        }
        filterChain.doFilter(request, response);
    }

    /** 判断当前请求是否为 OAuth2 authorize 端点。 */
    private boolean isAuthorizeEndpoint(HttpServletRequest request) {
        return "/oauth2/authorize".equals(request.getRequestURI());
    }

    /** 校验 session 渠道与 authorize 请求渠道是否一致。 */
    private boolean isChannelMatched(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object value = session.getAttribute(AuthSessionAttributes.LOGIN_CHANNEL);
        if (!(value instanceof String actualChannel) || !StringUtils.hasText(actualChannel)) {
            return false;
        }
        String clientId = request.getParameter("client_id");
        LoginChannel expected = resolveExpectedChannel(clientId);
        return expected.getValue().equals(actualChannel);
    }

    /** 将 authorize 请求的 client_id 映射为预期渠道。 */
    private LoginChannel resolveExpectedChannel(String clientId) {
        if (StringUtils.hasText(clientId) && clientId.equals(oauthClientsProperties.getPortal().getClientId())) {
            return LoginChannel.PORTAL;
        }
        return LoginChannel.ADMIN;
    }
}
