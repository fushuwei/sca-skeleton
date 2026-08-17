package io.github.fushuwei.scaskeleton.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置实体类
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SysConfig extends BaseEntity {

    // ==================== 基本信息 ====================

    /** 租户 ID */
    private String tenantId;

    /** 配置项名称 */
    private String name;

    // ==================== 配置项 ====================

    /** 配置键 */
    @TableField("`key`")
    private String configKey;

    /** 配置值 */
    @TableField("`value`")
    private String configValue;

    // ==================== 属性信息 ====================

    /** 类型 */
    private String type;

    /** 状态 */
    private Integer status;

    /** 是否内置 */
    private Integer isBuiltin;

    /** 备注 */
    private String remark;
}
