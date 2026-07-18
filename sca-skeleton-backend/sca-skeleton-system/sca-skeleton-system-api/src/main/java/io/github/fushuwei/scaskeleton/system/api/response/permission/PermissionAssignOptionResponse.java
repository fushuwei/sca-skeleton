package io.github.fushuwei.scaskeleton.system.api.response.permission;

import lombok.Data;

/**
 * 权限分配选项响应对象
 *
 * @author Fu Wei
 */
@Data
public class PermissionAssignOptionResponse {

    /** 权限 ID */
    private String id;

    /** 上级权限 ID（顶级为 "0"） */
    private String parentId;

    /** 权限名称 */
    private String name;

    /** 英文名称 */
    private String nameEn;

    /** 类型 */
    private String type;

    /** 图标 */
    private String icon;

    /** 排序号 */
    private Integer sort;
}
