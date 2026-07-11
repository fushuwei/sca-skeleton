package io.github.fushuwei.scaskeleton.system.api.request.tenant;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建租户请求对象。
 *
 * @author Fu Wei
 */
@Data
public class TenantCreateRequest {

    /** 租户名称 */
    @NotBlank(message = "租户名称不能为空")
    private String name;

    /** 租户编码 */
    @NotBlank(message = "租户编码不能为空")
    private String code;

    /** 套餐ID */
    @NotBlank(message = "套餐不能为空")
    private String packageId;

    /** 联系人姓名 */
    private String contactName;

    /** 联系人电话 */
    private String contactPhone;

    /** 联系人邮箱 */
    private String contactEmail;

    /** 绑定独立域名 */
    private String domainName;

    /** 账号数量限制，-1 表示不限 */
    @Min(value = -1, message = "账号数量限制不能小于 -1")
    private Integer accountLimit = -1;

    /** 过期时间（NULL表示永不过期） */
    private LocalDateTime expireTime;

    /** 租户状态（normal 正常，disabled 禁用） */
    @NotBlank(message = "租户状态不能为空")
    private String status;

    /** 备注 */
    private String remark;
}
