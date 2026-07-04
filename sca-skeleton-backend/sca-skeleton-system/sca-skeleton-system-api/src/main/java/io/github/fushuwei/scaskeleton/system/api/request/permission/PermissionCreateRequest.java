package io.github.fushuwei.scaskeleton.system.api.request.permission;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建权限请求对象。
 *
 * @author Fu Wei
 */
@Data
public class PermissionCreateRequest {

    /** 父节点 ID，顶级为 "0" */
    @NotBlank(message = "父节点ID不能为空")
    private String parentId;

    /** 权限名称 */
    @NotBlank(message = "权限名称不能为空")
    private String name;

    /** 类型：module-模块，folder-目录，menu-菜单，button-按钮 */
    @NotBlank(message = "权限类型不能为空")
    private String type;

    /** 权限标识，如 sys:user:list */
    private String code;
    /** 前端路由地址 */
    private String path;
    /** 前端组件路径 */
    private String component;
    /** 菜单图标 */
    private String icon;
    /** 排序号，越小越靠前 */
    private Integer sort;
    /** 是否可见：0-否，1-是 */
    private Integer isVisible;
    /** 是否外链：0-否，1-是 */
    private Integer isExternal;
    /** 状态：enabled / disabled */
    private String status;
    /** 备注 */
    private String remark;
}
