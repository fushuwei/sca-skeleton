package io.github.fushuwei.scaskeleton.security.annotation;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * {@link RequiresPermission} 的 SpEL 委托校验器
 *
 * @author Fu Wei
 */
public class RequiresPermissionAuthorizer {

    /**
     * 供 {@link RequiresPermission} 元注解 {@code @PreAuthorize} 调用的入口
     *
     * @param authentication 当前认证主体
     * @param annotation     方法或类上的 {@link RequiresPermission} 实例
     * @return 是否通过权限校验
     */
    public boolean check(Authentication authentication, RequiresPermission annotation) {
        if (annotation == null || annotation.value() == null || annotation.value().length == 0) {
            return false;
        }

        // 过滤空白权限编码
        String[] permissions = Arrays.stream(annotation.value())
            .filter(StringUtils::hasText)
            .toArray(String[]::new);
        if (permissions.length == 0) {
            return false;
        }

        // 单个权限：直接精确匹配
        if (permissions.length == 1) {
            return hasAuthority(authentication, permissions[0]);
        }

        // 多个权限：必须全部满足
        if (annotation.match() == RequiresPermission.MatchMode.ALL) {
            return Arrays.stream(permissions)
                .allMatch(permission -> hasAuthority(authentication, permission));
        }

        // 多个权限：满足任意一个
        return Arrays.stream(permissions)
            .anyMatch(permission -> hasAuthority(authentication, permission));
    }

    /**
     * 判断当前主体是否拥有指定权限编码
     *
     * @param authentication 认证主体
     * @param permission     权限编码
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
