package io.github.fushuwei.scaskeleton.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级权限校验注解
 * <p>
 * {@link PreAuthorize} 语法糖，底层仍使用 Spring Security 标准 {@code GrantedAuthority} 匹配语义，用法如下：
 * <ul>
 *   <li>单个权限（直接精确匹配）：{@code @RequiresPermission("sys:user:list")}</li>
 *   <li>多个权限（满足任意一个）：{@code @RequiresPermission({"sys:user:list", "sys:user:edit"})}</li>
 *   <li>多个权限（必须全部满足）：{@code @RequiresPermission(value = {"a", "b"}, match = MatchMode.ALL)}</li>
 * </ul>
 *
 * @author Fu Wei
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("@requiresPermissionAuthorizer.check(authentication, @annotation)")
public @interface RequiresPermission {

    /**
     * 权限编码，支持多个权限
     */
    String[] value();

    /**
     * 当 {@link #value()} 包含多个权限时的匹配方式，默认满足任意一个权限即可
     */
    MatchMode match() default MatchMode.ANY;

    /**
     * 多权限匹配策略
     */
    enum MatchMode {

        /**
         * 满足任意一个权限即可
         */
        ANY,

        /**
         * 必须同时拥有全部权限
         */
        ALL
    }
}
