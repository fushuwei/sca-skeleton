package io.github.fushuwei.scaskeleton.system.api.request.permission;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新权限请求对象
 *
 * @author Fu Wei
 */
@Data
public class PermissionUpdateRequest {

    // ==================== 基本信息 ====================

    /** 权限 ID */
    @NotBlank(message = "权限 ID 不能为空")
    private String id;

    /** 权限名称 */
    @NotBlank(message = "权限名称不能为空")
    private String name;

    /** 英文名称 */
    private String nameEn;

    // ==================== 前端配置 ====================

    /** 权限标识 */
    private String code;

    /** 前端路由地址 */
    private String path;

    /** 前端组件路径 */
    private String component;

    /** 图标 */
    private String icon;

    // ==================== 属性信息 ====================

    /** 排序号 */
    private Integer sort;

    /** 是否可见 */
    private Integer isVisible;

    /** 是否外链 */
    private Integer isExternal;

    /** 状态 */
    private String status;

    /** 备注 */
    private String remark;
}
