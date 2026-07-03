package io.github.fushuwei.scaskeleton.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户部门关联。
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_dept")
public class SysUserDept extends BaseEntity {

    private String tenantId;
    private String userId;
    private String deptId;
    /** 是否主部门：0-否，1-是 */
    private Integer isPrimary;
}
