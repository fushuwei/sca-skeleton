package io.github.fushuwei.scaskeleton.system.api.request.tenant;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建租户请求对象
 *
 * @author Fu Wei
 */
@Data
public class TenantCreateRequest {

    // ==================== 基本信息 ====================

    /** 租户名称 */
    @NotBlank(message = "租户名称不能为空")
    private String name;

    /** 租户编码 */
    @NotBlank(message = "租户编码不能为空")
    private String code;

    /** 套餐 ID */
    @NotBlank(message = "套餐不能为空")
    private String packageId;

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
    @NotBlank(message = "租户状态不能为空")
    private String status;

    /** 备注 */
    private String remark;
}
