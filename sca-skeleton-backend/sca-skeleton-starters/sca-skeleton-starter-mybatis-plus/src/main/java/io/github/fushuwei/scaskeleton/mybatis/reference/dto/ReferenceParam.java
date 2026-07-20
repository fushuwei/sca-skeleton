package io.github.fushuwei.scaskeleton.mybatis.reference.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 引用关系参数对象
 * <p>
 * 用于将 {@link io.github.fushuwei.scaskeleton.mybatis.reference.annotation.Reference} 注解的属性提取为标准 JavaBean
 *
 * @author Fu Wei
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceParam {

    /** 引用方表名，支持跨库全限定名（如 user_db.sys_user） */
    private String table;

    /** 引用方表中引用本实体的字段名，如 tenant_id */
    private String column;

    /** 校验失败时返回给用户的提示信息 */
    private String message;

    /** 引用方表是否使用逻辑删除字段（is_deleted），默认为 true，表示 SQL 会自动拼接 AND is_deleted = 0 条件 */
    private boolean logicalDelete;
}
