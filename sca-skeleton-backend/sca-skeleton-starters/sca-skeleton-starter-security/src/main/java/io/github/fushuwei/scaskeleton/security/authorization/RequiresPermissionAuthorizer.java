package io.github.fushuwei.scaskeleton.security.authorization;

import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * {@link RequiresPermission} 的 SpEL 委托校验器。
 * <p>
 * 将 {@link RequiresPermission#value()} 中的权限码映射为对 {@link Authentication#getAuthorities()} 的标准匹配，
 * 语义与 {@code hasAuthority} / {@code hasAnyAuthority} 一致。
 * <p>
 * Bean 名固定为 {@code requiresPermissionAuthorizer}，供注解元数据 {@code @PreAuthorize} SpEL 引用。
 *
 * @author Fu Wei
 */
public class RequiresPermissionAuthorizer {

    /**
     * 供 {@link RequiresPermission} 元注解 {@code @PreAuthorize} 调用的入口。
     *
     * @param authentication 当前认证主体
     * @param annotation     方法或类上的 {@link RequiresPermission} 实例
     * @return 是否通过权限校验
     */
    public boolean check(Authentication authentication, RequiresPermission annotation) {
        if (annotation == null || annotation.value() == null || annotation.value().length == 0) {
            return false;
        }
        String[] permissions = Arrays.stream(annotation.value())
            .filter(StringUtils::hasText)
            .toArray(String[]::new);
        if (permissions.length == 0) {
            return false;
        }
        // 仅一个权限：与 hasAuthority 等价
        if (permissions.length == 1) {
            return hasAuthority(authentication, permissions[0]);
        }
        // 多个权限：按 match 策略做 OR / AND
        if (annotation.match() == RequiresPermission.MatchMode.ALL) {
            return Arrays.stream(permissions)
                .allMatch(permission -> hasAuthority(authentication, permission));
        }
        return Arrays.stream(permissions)
            .anyMatch(permission -> hasAuthority(authentication, permission));
    }

    /**
     * 判断当前主体是否拥有指定权限码（精确匹配 {@link GrantedAuthority#getAuthority()}）。
     *
     * @param authentication 认证主体，未登录时为 null
     * @param permission     权限码
     * @return 是否拥有该权限
     */
    private static boolean hasAuthority(Authentication authentication, String permission) {
        if (authentication == null || !StringUtils.hasText(permission)) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (permission.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
