package io.github.fushuwei.scaskeleton.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级权限校验注解，底层仍使用 Spring Security 标准 {@code GrantedAuthority} 匹配语义，用法如下：
 * <ul>
 *   <li>单个权限（直接精确匹配）：{@code @RequiresPermission("sys:user:list")}</li>
 *   <li>多个权限（必须全部满足）：{@code @RequiresPermission(value = {"a", "b"}, logical = Logical.AND)}</li>
 *   <li>多个权限（满足任意一个）：{@code @RequiresPermission(value = {"a", "b"}, logical = Logical.OR)}</li>
 * </ul>
 *
 * @author Fu Wei
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {

    /**
     * 权限编码，支持多个权限
     */
    String[] value();

    /**
     * 当 {@link #value()} 包含多个权限时的逻辑关系，默认 OR（必须同时拥有全部权限）
     */
    Logical logical() default Logical.OR;

    /**
     * 多权限逻辑关系
     */
    enum Logical {

        /**
         * 必须同时拥有全部权限
         */
        AND,

        /**
         * 满足任意一个权限即可
         */
        OR
    }
}
