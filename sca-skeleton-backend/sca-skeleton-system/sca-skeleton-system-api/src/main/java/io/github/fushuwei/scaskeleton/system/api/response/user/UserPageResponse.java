package io.github.fushuwei.scaskeleton.system.api.response.user;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户分页查询响应对象
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageResponse extends UserResponse {

    /** 主部门名称 */
    private String deptName;

    /** 角色名称列表 */
    private String roleNames;
}
