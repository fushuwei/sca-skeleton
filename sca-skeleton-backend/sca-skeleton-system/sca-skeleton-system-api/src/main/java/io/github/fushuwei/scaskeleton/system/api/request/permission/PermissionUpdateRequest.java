package io.github.fushuwei.scaskeleton.system.api.request.permission;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新权限请求对象。
 * <p>
 * 父节点（parentId）和权限类型（type）创建后不可修改，故不包含在此对象中。
 *
 * @author Fu Wei
 */
@Data
public class PermissionUpdateRequest {

    /** 权限 ID */
    @NotBlank(message = "权限ID不能为空")
    private String id;

    /** 权限名称 */
    @NotBlank(message = "权限名称不能为空")
    private String name;

    /** 英文菜单名称，用于国际化 */
    private String nameEn;

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
