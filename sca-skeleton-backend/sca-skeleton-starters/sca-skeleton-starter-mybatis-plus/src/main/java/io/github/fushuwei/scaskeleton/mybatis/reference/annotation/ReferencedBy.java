package io.github.fushuwei.scaskeleton.mybatis.reference.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明实体被哪些表引用，用于删除前引用校验
 * <p>
 * 标注在被引用的实体类上，框架在删除该实体的数据前会检查所有引用表，若存在引用记录则抛出 {@code ResultCode.DATA_REFERENCED} 阻止删除
 * <p>
 * 使用示例：
 * <pre>
 * &#064;ReferencedBy(
 *     value = {
 *         &#064;Reference(table = "user_db.sys_user", column = "tenant_id", message = "租户下存在用户，无法删除"),
 *         &#064;Reference(table = "role_db.sys_role", column = "tenant_id", message = "租户下存在角色，无法删除")
 *     },
 *     displayColumn = "name"
 * )
 * </pre>
 * <p>
 * 未标注本注解的实体，删除时自动跳过引用校验
 *
 * @author Fu Wei
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReferencedBy {

    /**
     * 引用关系列表
     *
     * @return 引用关系数组
     */
    Reference[] value();

    /**
     * 被删除实体的展示字段名，用于校验不通过提示中替代 ID 显示，增强提示信息的可读性
     *
     * @return 展示字段名，默认展示 "name" 字段值
     */
    String displayColumn() default "name";
}
