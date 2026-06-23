package io.github.fushuwei.scaskeleton.security.annotation;

import io.github.fushuwei.scaskeleton.core.exception.ForbiddenException;
import io.github.fushuwei.scaskeleton.security.constant.OAuth2AccessTokenClaimNames;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * {@link RequiresPermission} 的权限校验委托器
 * <p>
 * 由 {@link RequiresPermissionAspect} 在方法执行前调用，基于当前认证主体的
 * {@code GrantedAuthority} 集合进行权限匹配；权限不足时抛出 {@link ForbiddenException}，
 * 交由 {@code GlobalExceptionHandler} 统一处理，返回标准 403 响应。
 * <p>
 * 平台超级管理员（{@code user_type = "superadmin"}）跳过一切权限校验，直接放行。
 *
 * @author Fu Wei
 */
public class RequiresPermissionChecker {

    /**
     * 平台超级管理员用户类型，拥有全部权限无需校验
     */
    private static final String USER_TYPE_SUPERADMIN = "superadmin";

    /**
     * 校验当前认证主体是否满足 {@link RequiresPermission} 声明的权限要求
     *
     * @param annotation 方法或类上的 {@link RequiresPermission} 实例
     * @throws ForbiddenException 权限不满足时抛出，由全局异常链路处理为 403
     */
    public void check(RequiresPermission annotation) {
        check(SecurityContextHolder.getContext().getAuthentication(), annotation);
    }

    /**
     * 校验指定认证主体是否满足 {@link RequiresPermission} 声明的权限要求
     *
     * @param authentication 当前认证主体
     * @param annotation     方法或类上的 {@link RequiresPermission} 实例
     * @throws ForbiddenException 权限不满足时抛出，由全局异常链路处理为 403
     */
    public void check(Authentication authentication, RequiresPermission annotation) {
        // 平台超级管理员直接放行，无需权限校验
        if (isSuperAdmin(authentication)) {
            return;
        }

        if (annotation == null || annotation.value() == null || annotation.value().length == 0) {
            // 未声明权限要求时直接拒绝，避免误配置导致越权
            throw new ForbiddenException("权限校验失败：未配置有效的权限编码");
        }

        // 过滤空白无效的权限编码
        String[] permissions = Arrays.stream(annotation.value()).filter(StringUtils::hasText).toArray(String[]::new);
        if (permissions.length == 0) {
            throw new ForbiddenException("权限校验失败：未配置有效的权限编码");
        }

        // 单个权限：直接精确匹配
        boolean granted;
        if (permissions.length == 1) {
            granted = hasAuthority(authentication, permissions[0]);
        } else if (annotation.logical() == RequiresPermission.Logical.AND) {
            // 多个权限：必须全部满足
            granted = Arrays.stream(permissions)
                .allMatch(permission -> hasAuthority(authentication, permission));
        } else {
            // 多个权限：满足任意一个
            granted = Arrays.stream(permissions)
                .anyMatch(permission -> hasAuthority(authentication, permission));
        }

        if (!granted) {
            throw new ForbiddenException("权限不足，缺少权限：" + Arrays.toString(permissions));
        }
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

    /**
     * 判断当前认证主体是否为平台超级管理员
     * <p>
     * 从不透明令牌自省结果中的 {@code user_type} claim 读取用户类型，
     * 若值为 {@code "superadmin"} 则跳过一切权限校验。
     *
     * @param authentication 当前认证主体
     * @return true 表示当前用户为平台超级管理员
     */
    private static boolean isSuperAdmin(Authentication authentication) {
        if (authentication != null
            && authentication.getPrincipal() instanceof OAuth2AuthenticatedPrincipal principal) {
            return USER_TYPE_SUPERADMIN.equals(
                principal.getAttributes().get(OAuth2AccessTokenClaimNames.USER_TYPE));
        }
        return false;
    }
}
