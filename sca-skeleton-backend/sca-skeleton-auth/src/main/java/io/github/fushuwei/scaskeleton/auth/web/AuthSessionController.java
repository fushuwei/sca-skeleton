package io.github.fushuwei.scaskeleton.auth.web;

import io.github.fushuwei.scaskeleton.auth.config.properties.OAuthClientsProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Auth 统一退出端点：后端统一编排会话销毁与双令牌吊销。
 *
 * @author Fu Wei
 */
@RestController
@RequiredArgsConstructor
public class AuthSessionController {

    /** OAuth 客户端配置（用于按 channel 回跳到对应登录页）。 */
    private final OAuthClientsProperties oauthClientsProperties;
    /** 授权存储服务（Redis OAuth2AuthorizationService），用于按 token 删除授权记录。 */
    private final OAuth2AuthorizationService oAuth2AuthorizationService;

    /**
     * 统一退出入口：
     * 1) 吊销 access_token / refresh_token（如有）；
     * 2) 销毁 Auth Session；
     * 3) 清理 SecurityContext；
     * 4) 重定向到对应登录页。
     *
     * @param channel admin 或 portal
     * @param accessToken 可选：当前 access_token（前端通过 POST 表单提交）
     * @param refreshToken 可选：当前 refresh_token（前端通过 POST 表单提交）
     */
    @GetMapping("/logout")
    @PostMapping("/logout")
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = "channel", required = false) String channel,
            @RequestParam(value = "access_token", required = false) String accessToken,
            @RequestParam(value = "refresh_token", required = false) String refreshToken) throws IOException {
        // 防止 access/refresh 指向同一 authorization 时重复删除。
        Set<String> removedAuthorizationIds = new HashSet<>();
        // 优先使用表单提交的 access_token；未提供时尝试从 Authorization 头提取 Bearer。
        revokeTokenIfPresent(resolveAccessToken(request, accessToken), OAuth2TokenType.ACCESS_TOKEN,
                removedAuthorizationIds);
        // refresh_token 仅从表单参数读取，避免写入 URL。
        revokeTokenIfPresent(refreshToken, OAuth2TokenType.REFRESH_TOKEN, removedAuthorizationIds);
        // 销毁服务端 Session（含保存的 pending authorize 与登录渠道）。
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        // 清理线程上下文认证信息，保证当前请求后续链路不再携带旧身份。
        SecurityContextHolder.clearContext();
        // 按渠道回到对应登录页（admin / portal）。
        String clientId = resolveClientId(channel);
        response.sendRedirect(oauthClientsProperties.resolveExternalLoginUrl(clientId));
    }

    /** 解析 access token：参数优先，回退 Authorization: Bearer。 */
    private String resolveAccessToken(HttpServletRequest request, String accessToken) {
        if (StringUtils.hasText(accessToken)) {
            return accessToken;
        }
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        if (!authorization.startsWith("Bearer ")) {
            return null;
        }
        String bearer = authorization.substring(7);
        return StringUtils.hasText(bearer) ? bearer : null;
    }

    /** 按 token 类型删除授权记录，实现 revoke 语义。 */
    private void revokeTokenIfPresent(String tokenValue, OAuth2TokenType tokenType, Set<String> removedAuthorizationIds) {
        if (!StringUtils.hasText(tokenValue)) {
            return;
        }
        OAuth2Authorization authorization = oAuth2AuthorizationService.findByToken(tokenValue, tokenType);
        if (authorization == null) {
            return;
        }
        if (!removedAuthorizationIds.add(authorization.getId())) {
            return;
        }
        oAuth2AuthorizationService.remove(authorization);
    }

    /** 将 channel 参数映射为对应 clientId，未知值默认 admin。 */
    private String resolveClientId(String channel) {
        if (StringUtils.hasText(channel) && "portal".equalsIgnoreCase(channel)) {
            return oauthClientsProperties.getPortal().getClientId();
        }
        return oauthClientsProperties.getAdmin().getClientId();
    }
}

