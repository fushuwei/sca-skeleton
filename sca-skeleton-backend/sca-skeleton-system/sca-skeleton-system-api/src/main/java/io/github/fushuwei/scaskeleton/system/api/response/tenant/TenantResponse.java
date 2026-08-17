package io.github.fushuwei.scaskeleton.system.api.response.tenant;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户响应对象
 *
 * @author Fu Wei
 */
@Data
public class TenantResponse {

    // ==================== 基本信息 ====================

    /** 租户 ID */
    private String id;

    /** 租户名称 */
    private String name;

    /** 租户编码 */
    private String code;

    // ==================== 套餐信息 ====================

    /** 套餐 ID */
    private String packageId;

    /** 套餐名称 */
    private String packageName;

    // ==================== 联系信息 ====================

    /** 联系人 */
    private String contactName;

    /** 联系电话 */
    private String contactPhone;

    /** 联系邮箱 */
    private String contactEmail;

    /** 域名 */
    private String domainName;

    // ==================== 属性信息 ====================

    /** 生效时间（null 表示立即生效） */
    private LocalDateTime effectiveTime;

    /** 过期时间（null 表示永不过期） */
    private LocalDateTime expireTime;

    /** 状态 */
    private String status;

    /** 是否内置 */
    private Integer isBuiltin;

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
