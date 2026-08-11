package io.github.fushuwei.scaskeleton.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色自定义数据权限（部门）关联实体类
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_data_scope")
public class SysRoleDataScope extends BaseEntity {

    /** 租户 ID */
    private String tenantId;

    /** 角色 ID */
    private String roleId;

    /** 部门 ID */
    private String deptId;
}
