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
 * 底层仍使用 Spring Security 标准 {@code hasAuthority} 表达式，不引入自定义校验规则；
 * 仅将 {@code @PreAuthorize("hasAuthority('xxx')")} 简化为一行 {@code @RequiresPermission("xxx")}。
 * <p>
 * 示例：{@code @RequiresPermission("sys:user:list")} 等价于
 * {@code @PreAuthorize("hasAuthority('sys:user:list')")}。
 *
 * @author Fu Wei
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasAuthority(@annotation.value())")
public @interface RequiresPermission {

    /**
     * 权限码，与 RBAC 中 permission 标识一致（不带 ROLE_ 前缀）。
     */
    String value();
}
