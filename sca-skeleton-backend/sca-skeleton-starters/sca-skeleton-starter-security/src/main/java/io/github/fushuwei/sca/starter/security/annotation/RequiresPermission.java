package io.github.fushuwei.sca.starter.security.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级权限校验注解（{@link PreAuthorize} 的语法糖）。
 * <p>
 * 底层仍使用 Spring Security 标准 {@code GrantedAuthority} 匹配语义（与 {@code hasAuthority} /
 * {@code hasAnyAuthority} 一致），不引入自定义权限模型。
 * <p>
 * {@link #value()} 为数组类型，遵循 Java 注解简写规则：
 * <ul>
 *   <li>单个权限：{@code @RequiresPermission("sys:user:list")}（可省略花括号）</li>
 *   <li>多个权限：{@code @RequiresPermission({"sys:user:list", "sys:user:edit"})}</li>
 *   <li>全部满足：{@code @RequiresPermission(value = {"a", "b"}, match = MatchMode.ALL)}</li>
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
     * 权限码列表。仅一个元素时等价于单权限 {@code hasAuthority}；多个元素时由 {@link #match()} 决定 AND / OR。
     */
    String[] value();

    /**
     * 当 {@link #value()} 包含多个权限时的匹配方式，默认满足任意一个即可。
     */
    MatchMode match() default MatchMode.ANY;

    /**
     * 多权限匹配策略。
     */
    enum MatchMode {

        /**
         * 满足任意一个权限即可（等价于 {@code hasAnyAuthority}）。
         */
        ANY,

        /**
         * 必须同时拥有全部权限（等价于多个 {@code hasAuthority} 的 AND）。
         */
        ALL
    }
}
