package io.github.fushuwei.sca.system.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.fushuwei.sca.starter.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户角色关联。
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_role")
public class SysUserRole extends BaseEntity {

    private String tenantId;
    private String userId;
    private String roleId;
}
