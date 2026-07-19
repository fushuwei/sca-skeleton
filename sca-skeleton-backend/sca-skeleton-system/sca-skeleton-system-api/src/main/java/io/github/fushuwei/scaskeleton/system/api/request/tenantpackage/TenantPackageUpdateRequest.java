package io.github.fushuwei.scaskeleton.system.api.request.tenantpackage;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 更新租户套餐请求对象
 *
 * @author Fu Wei
 */
@Data
public class TenantPackageUpdateRequest {

    // ==================== 基本信息 ====================

    /** 套餐 ID */
    @NotBlank(message = "套餐 ID 不能为空")
    private String id;

    /** 套餐名称 */
    @NotBlank(message = "套餐名称不能为空")
    private String name;

    /** 套餐编码 */
    @NotBlank(message = "套餐编码不能为空")
    private String code;

    /** 状态 */
    @NotBlank(message = "套餐状态不能为空")
    private String status;

    // ==================== 配额信息 ====================

    /** 用户数限制（-1 表示不限） */
    @Min(value = -1, message = "用户数限制不能小于 -1")
    private Integer userLimit = -1;

    /** API 调用限制/日（-1 表示不限） */
    @Min(value = -1, message = "API 调用限制不能小于 -1")
    private Integer apiLimit = -1;

    /** 存储限制(GB)（-1 表示不限） */
    @Min(value = -1, message = "存储限制不能小于 -1")
    private Integer storageLimit = -1;

    /** 有效期天数（-1 表示不限） */
    @Min(value = -1, message = "有效期天数不能小于 -1")
    private Integer expireDays = -1;

    // ==================== 属性信息 ====================

    /** 排序号 */
    private Integer sort;

    /** 备注 */
    private String remark;

    // ==================== 关联信息 ====================

    /** 权限 ID 列表 */
    private List<String> permissionIds;

    // ==================== 乐观锁 ====================

    /** 乐观锁版本号 */
    private Integer version;
}
