package io.github.fushuwei.scaskeleton.system.api.response.role;

import lombok.Data;

/**
 * 角色选项响应对象
 *
 * @author Fu Wei
 */
@Data
public class RoleOptionResponse {

    /** 角色 ID */
    private String id;

    /** 角色名称 */
    private String name;

    /** 角色编码 */
    private String code;

    /** 排序号 */
    private Integer sort;

    /** 角色域 */
    private String realm;
}
