package io.github.fushuwei.sca.starter.security.context;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Security 上下文工具类。
 * <p>
 * 提供从当前线程 SecurityContext 中提取 JWT Claims 的静态方法，
 * 可在 Controller、Service 层直接调用，无需注入 Security 相关依赖。
 * <p>
 * 注意：此工具类依赖 Spring Security 的 ThreadLocal SecurityContext，
 * 在异步线程中需要配合 {@code DelegatingSecurityContextExecutor} 或手动传递上下文。
 *
 * @author Fu Wei
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前认证的 JWT 对象。
     *
     * @return JWT 实例，未认证或认证类型不匹配时返回 {@code null}
     */
    public static Jwt getCurrentJwt() {
        // 从当前线程的 SecurityContext 中获取认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 仅处理 JWT 认证类型，其他认证方式（如 UsernamePassword）返回 null
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken();
        }
        return null;
    }

    /**
     * 从 JWT Claims 中获取当前用户 ID（对应 sub 字段或自定义 Claims）。
     *
     * @return 用户 ID 字符串，未认证时返回 {@code null}
     */
    public static String getCurrentUserId() {
        Jwt jwt = getCurrentJwt();
        // JWT 为 null 表示当前请求未认证
        if (jwt == null) {
            return null;
        }
        // sub 字段是 JWT 标准 Subject，通常存储用户唯一标识
        return jwt.getSubject();
    }

    /**
     * 从 JWT Claims 中获取指定字段值。
     *
     * @param claimName JWT Claims 字段名
     * @return 字段值，字段不存在或未认证时返回 {@code null}
     */
    public static String getClaim(String claimName) {
        Jwt jwt = getCurrentJwt();
        if (jwt == null) {
            return null;
        }
        // 从 JWT Claims Map 中获取指定字段，转换为字符串返回
        Object claim = jwt.getClaims().get(claimName);
        return claim != null ? claim.toString() : null;
    }

    /**
     * 从 JWT Claims 中获取当前登录用户名（preferred_username）。
     *
     * @return 用户名，未认证时返回 {@code null}
     */
    public static String getUsername() {
        return getClaim("preferred_username");
    }

    /**
     * 从 JWT Claims 中获取当前用户的租户 ID（tenant_id）。
     *
     * @return 租户 ID，未认证或无该字段时返回 {@code null}
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
        // authentication 不为 null 且 isAuthenticated() 为 true 才算真正认证
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);
    }
}
