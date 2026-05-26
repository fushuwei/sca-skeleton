package io.github.fushuwei.scaskeleton.security.context;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Map;

/**
 * Security 上下文工具类。
 * <p>
 * 从当前线程 SecurityContext 读取不透明令牌自省后的属性或 JWT Claims（兼容双模式），
 * 供 Controller、Service 直接获取用户上下文。
 *
 * @author Fu Wei
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前 JWT（仅当认证类型为 {@link JwtAuthenticationToken} 时）。
     *
     * @return JWT；非 JWT 资源服务器时返回 null
     */
    public static Jwt getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 仅 JWT 资源服务器模式返回 token
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken();
        }
        return null;
    }

    /**
     * 获取当前请求的 token 属性 Map：不透明令牌为 {@link BearerTokenAuthentication#getTokenAttributes()}，
     * JWT 为 {@link Jwt#getClaims()}。
     *
     * @return 属性 Map；未认证或类型不支持时返回 null
     */
    public static Map<String, Object> getTokenAttributes() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 不透明令牌：自省 claims 在 tokenAttributes 中
        if (authentication instanceof BearerTokenAuthentication bearer) {
            return bearer.getTokenAttributes();
        }
        // JWT：claims 在 token 内
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getClaims();
        }
        return null;
    }

    /**
     * 从 token 属性或 JWT 中获取用户 ID（默认 sub）。
     *
     * @return 用户 ID；未认证时返回 null
     */
    public static String getCurrentUserId() {
        Map<String, Object> attrs = getTokenAttributes();
        if (attrs == null) {
            return null;
        }
        Object sub = attrs.get("sub");
        return sub != null ? sub.toString() : null;
    }

    /**
     * 从 token 属性或 JWT 中读取指定声明/字段的字符串形式。
     *
     * @param claimName 字段名
     * @return 字符串值；缺失或未认证时返回 null
     */
    public static String getClaim(String claimName) {
        Map<String, Object> attrs = getTokenAttributes();
        if (attrs == null) {
            return null;
        }
        Object claim = attrs.get(claimName);
        return claim != null ? claim.toString() : null;
    }

    /**
     * 获取当前登录用户名（preferred_username）。
     *
     * @return 用户名；未认证时返回 null
     */
    public static String getUsername() {
        return getClaim("preferred_username");
    }

    /**
     * 获取租户 ID（tenant_id）。
     *
     * @return 租户 ID；未认证或无该字段时返回 null
     */
    public static String getTenantId() {
        return getClaim("tenant_id");
    }

    /**
     * 判断当前请求是否已通过认证。
     *
     * @return true 表示已认证
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 排除匿名用户
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);
    }
}
