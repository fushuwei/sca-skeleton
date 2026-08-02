package io.github.fushuwei.scaskeleton.datasource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SQL 查询模板实体类
 * <p>
 * 按租户隔离。${param} 必须绑定 PreparedStatement 占位符，禁止字符串拼接。
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ds_sql_template")
public class SqlTemplate extends BaseEntity {

    /** 租户 ID */
    private String tenantId;

    /** 关联数据源 ID */
    private String datasourceId;

    /** 模板名称 */
    private String name;

    /** 模板描述 */
    private String description;

    /** SQL 内容（支持 ${param} 占位符） */
    private String sqlContent;

    /** 参数定义（JSON 数组） */
    private String parameters;

    /** 分类 */
    private String category;

    /** 标签（逗号分隔） */
    private String tags;

    /** 是否内置（0 否，1 是） */
    private Integer isBuiltin;

    /** 状态（enabled/disabled） */
    private String status;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
