package io.github.fushuwei.scaskeleton.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色实体。
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    private String tenantId;
    private String name;
    private String code;
    /**
     * 数据权限范围：
     * all-全部，tenant-租户，dept_and_sub-本部门及下级，dept-仅本部门，
     * personal-仅本人，custom-自定义
     */
    private String dataScope;
    /** 是否系统内置：0-否，1-是，内置角色不允许删除 */
    private Integer isBuiltin;
    private Integer sort;
    private String remark;

    /** 关联的权限数量（非表字段，仅用于接收 SQL 查询结果） */
    @TableField(exist = false)
    private Integer permissionCount;

    @Version
    private Integer version;
}
