package io.github.fushuwei.scaskeleton.system.api.response.role;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色响应对象
 *
 * @author Fu Wei
 */
@Data
public class RoleResponse {

    // ==================== 基本信息 ====================

    /** 角色 ID */
    private String id;

    /** 租户 ID */
    private String tenantId;

    /** 角色名称 */
    private String name;

    /** 角色编码 */
    private String code;

    // ==================== 属性信息 ====================

    /** 数据权限范围 */
    private String dataScope;

    /** 是否系统内置 */
    private Integer isBuiltin;

    /** 排序号 */
    private Integer sort;

    /** 备注 */
    private String remark;

    /** 乐观锁版本号 */
    private Integer version;

    // ==================== 关联信息 ====================

    /** 关联权限数量 */
    private Integer permissionCount;

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
