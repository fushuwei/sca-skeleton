package io.github.fushuwei.scaskeleton.system.api.response.user;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户分页查询响应对象（在 UserResponse 基础上扩展部门名称和角色名称）。
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageResponse extends UserResponse {

    /** 主部门名称（通过 sys_user_dept LEFT JOIN sys_dept 获取） */
    private String deptName;

    /** 角色名称列表（逗号分隔，通过 sys_user_role LEFT JOIN sys_role 获取） */
    private String roleNames;
}
