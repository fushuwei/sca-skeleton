package io.github.fushuwei.scaskeleton.system.api.dto.user;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户分页查询视图对象（在 UserVO 基础上扩展部门名称和角色名称）。
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageVO extends UserVO {

    /** 主部门名称（通过 sys_user_dept LEFT JOIN sys_dept 获取） */
    private String deptName;

    /** 角色名称列表（逗号分隔，通过 sys_user_role LEFT JOIN sys_role 获取） */
    private String roleNames;
}
