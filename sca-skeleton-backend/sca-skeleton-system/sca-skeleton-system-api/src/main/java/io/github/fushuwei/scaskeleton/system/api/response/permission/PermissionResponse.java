package io.github.fushuwei.scaskeleton.system.api.response.permission;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限响应对象
 *
 * @author Fu Wei
 */
@Data
public class PermissionResponse {

    // ==================== 基本信息 ====================

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

    /** 权限编码 */
    private String code;

    /** 权限域 */
    private String realm;

    // ==================== 前端配置 ====================

    /** 前端路由地址 */
    private String path;

    /** 前端组件路径 */
    private String component;

    /** 图标 */
    private String icon;

    // ==================== 属性信息 ====================

    /** 是否可见 */
    private Integer isVisible;

    /** 是否外链 */
    private Integer isExternal;

    /** 状态 */
    private String status;

    /** 层级路径 */
    private String treePath;

    /** 排序号 */
    private Integer sort;

    /** 备注 */
    private String remark;

    /** 乐观锁版本号 */
    private Integer version;

    // ==================== 审计字段 ====================

    /** 创建人 ID */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新人 ID */
    private String updateBy;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
