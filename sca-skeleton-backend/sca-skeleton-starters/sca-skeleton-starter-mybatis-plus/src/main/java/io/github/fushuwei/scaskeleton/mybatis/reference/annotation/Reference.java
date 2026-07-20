package io.github.fushuwei.scaskeleton.mybatis.reference.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 描述一条引用关系，用于 {@link ReferencedBy} 注解内部
 *
 * @author Fu Wei
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Reference {

    /**
     * 引用方表名
     * <p>
     * 支持跨库全限定名，如 {@code user_db.sys_user}，当前架构为单 MySQL 实例多库，跨库查询可直接使用
     *
     * @return 表名
     */
    String table();

    /**
     * 引用方表中引用本实体的字段名，如 {@code tenant_id}
     *
     * @return 字段名
     */
    String column();

    /**
     * 校验失败时返回给用户的提示信息，如 "租户下存在用户，无法删除"
     *
     * @return 提示信息
     */
    String message();

    /**
     * 引用方表是否使用逻辑删除字段（is_deleted）
     * <p>
     * 默认 true：SQL 自动拼接 {@code AND is_deleted = 0}，仅统计未删除的引用记录
     * <p>
     * 设为 false 的场景：引用方表无 {@code is_deleted} 字段，此时跳过条件拼接，避免 MySQL 抛 {@code Unknown column 'is_deleted'} 错误
     *
     * @return 是否拼接逻辑删除条件
     */
    boolean logicalDelete() default true;
}
