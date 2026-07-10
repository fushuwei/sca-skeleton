package io.github.fushuwei.scaskeleton.system.api.request.tenantpackage;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 创建租户套餐请求对象。
 *
 * @author Fu Wei
 */
@Data
public class TenantPackageCreateRequest {

    /** 套餐名称 */
    @NotBlank(message = "套餐名称不能为空")
    private String name;

    /** 套餐编码 */
    @NotBlank(message = "套餐编码不能为空")
    private String code;

    /** 套餐状态（enabled 启用，disabled 禁用） */
    @NotBlank(message = "套餐状态不能为空")
    private String status;

    /** 用户数限制，-1 表示不限 */
    @Min(value = -1, message = "用户数限制不能小于 -1")
    private Integer userLimit = -1;

    /** API 调用限制/日，-1 表示不限 */
    @Min(value = -1, message = "API调用限制不能小于 -1")
    private Integer apiLimit = -1;

    /** 存储限制(GB)，-1 表示不限 */
    @Min(value = -1, message = "存储限制不能小于 -1")
    private Integer storageLimit = -1;

    /** 有效期天数，-1 表示不限 */
    @Min(value = -1, message = "有效期天数不能小于 -1")
    private Integer expireDays = -1;

    /** 排序号，越小越靠前 */
    private Integer sort;

    /** 备注 */
    private String remark;

    /** 权限 ID 列表（全量替换），创建时一并提交 */
    private List<String> permissionIds;
}
