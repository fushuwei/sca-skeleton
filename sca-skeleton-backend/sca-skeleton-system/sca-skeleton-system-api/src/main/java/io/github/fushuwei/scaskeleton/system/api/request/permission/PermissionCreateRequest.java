package io.github.fushuwei.scaskeleton.system.api.request.permission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 创建权限请求对象
 *
 * @author Fu Wei
 */
@Data
public class PermissionCreateRequest {

    // ==================== 基本信息 ====================

    /** 上级权限 ID（顶级为 "0"） */
    @NotBlank(message = "上级权限 ID 不能为空")
    private String parentId;

    /** 权限名称 */
    @NotBlank(message = "权限名称不能为空")
    private String name;

    /** 英文名称 */
    private String nameEn;

    /** 类型 */
    @NotBlank(message = "权限类型不能为空")
    private String type;

    /** 权限域 */
    @NotBlank(message = "权限域不能为空")
    @Pattern(regexp = "^(admin|portal)$", message = "权限域只能是 admin 或 portal")
    private String realm;

    // ==================== 前端配置 ====================

    /** 权限编码 */
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
