package io.github.fushuwei.scaskeleton.system.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置实体。
 * <p>
 * 注意：数据库列 {@code key} 和 {@code value} 均为 SQL 保留字，
 * 需通过 {@code @TableField} 显式映射，MyBatis-Plus 会自动加反引号处理。
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SysConfig extends BaseEntity {

    private String tenantId;
    private String name;

    /** 配置键（映射 SQL 保留字列 `key`） */
    @TableField("`key`")
    private String configKey;

    /** 配置值（映射 SQL 保留字列 `value`） */
    @TableField("`value`")
    private String configValue;

    /** 类型：string / number / boolean / json */
    private String type;
    private Integer status;
    private Integer isBuiltin;
    private String remark;
}
