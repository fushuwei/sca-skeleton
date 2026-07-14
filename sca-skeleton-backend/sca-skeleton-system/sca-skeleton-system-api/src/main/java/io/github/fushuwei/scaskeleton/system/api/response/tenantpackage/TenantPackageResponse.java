package io.github.fushuwei.scaskeleton.system.api.response.tenantpackage;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户套餐响应对象
 *
 * @author Fu Wei
 */
@Data
public class TenantPackageResponse {

    // ==================== 基本信息 ====================

    /** 套餐 ID */
    private String id;

    /** 套餐名称 */
    private String name;

    /** 套餐编码 */
    private String code;

    /** 状态 */
    private String status;

    // ==================== 配额信息 ====================

    /** 用户数限制（-1 表示不限） */
    private Integer userLimit;

    /** API 调用限制/日（-1 表示不限） */
    private Integer apiLimit;

    /** 存储限制(GB)（-1 表示不限） */
    private Integer storageLimit;

    /** 有效期天数（-1 表示不限） */
    private Integer expireDays;

    // ==================== 属性信息 ====================

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
