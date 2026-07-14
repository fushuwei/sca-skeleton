package io.github.fushuwei.scaskeleton.security.context;

import io.github.fushuwei.scaskeleton.security.constant.OAuth2AccessTokenClaimNames;
import lombok.experimental.UtilityClass;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;

import java.util.Map;

/**
 * Security 上下文工具类
 * <p>
 * 从当前线程 SecurityContext 读取不透明令牌自省后的属性，供 Controller、Service 直接获取用户上下文
 *
 * @author Fu Wei
 */
@UtilityClass
public class SecurityUtils {

    /**
     * 获取当前请求的 token 自省后的属性集合
     *
     * @return token 自省后的属性集合，未认证时返回 null
     */
    public static Map<String, Object> getTokenAttributes() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof BearerTokenAuthentication bearer) {
            return bearer.getTokenAttributes();
        }
        return null;
    }

    /**
     * 从 token 自省属性中读取指定字段的声明
     *
     * @param claimName 字段名
     * @return 指定声明的值，缺失或未认证时返回 null
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
     * 从 token 自省属性中获取租户 ID
     *
     * @return 租户 ID，未认证时返回 null
     */
    public static String getTenantId() {
        return getClaim(OAuth2AccessTokenClaimNames.TENANT_ID);
    }

    /**
     * 从 token 自省属性中获取用户 ID
     *
     * @return 用户 ID，未认证时返回 null
     */
    public static String getUserId() {
        return getClaim(OAuth2AccessTokenClaimNames.SUB);
    }

    /**
     * 从 token 自省属性中获取用户名
     *
     * @return 用户名，未认证时返回 null
     */
    public static String getUsername() {
        return getClaim(OAuth2AccessTokenClaimNames.PREFERRED_USERNAME);
    }

    /**
     * 从 token 自省属性中获取用户昵称
     *
     * @return 用户名，未认证时返回 null
     */
    public static String getNickname() {
        return getClaim(OAuth2AccessTokenClaimNames.NICKNAME);
    }

    /**
     * 判断当前认证用户是否为平台超级管理员
     * <p>
     * 从不透明令牌自省属性中读取 {@code is_superadmin} claim，值为 {@code "1"} 时返回 true
     *
     * @return true 表示当前用户为平台超级管理员，未认证或非超管时返回 false
     */
    public static boolean isSuperAdmin() {
        return "1".equals(getClaim(OAuth2AccessTokenClaimNames.IS_SUPER_ADMIN));
    }

    /**
     * 判断当前请求是否已通过认证（排除了匿名用户）
     *
     * @return true 表示已认证，false 表示未认证
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
