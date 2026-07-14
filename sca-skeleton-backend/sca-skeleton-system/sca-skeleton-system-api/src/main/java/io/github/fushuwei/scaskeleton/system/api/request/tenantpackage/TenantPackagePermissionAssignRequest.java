package io.github.fushuwei.scaskeleton.system.api.request.tenantpackage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 套餐权限分配请求对象
 *
 * @author Fu Wei
 */
@Data
public class TenantPackagePermissionAssignRequest {

    /** 套餐 ID */
    @NotBlank(message = "套餐 ID 不能为空")
    private String id;

    /** 权限 ID 列表 */
    @NotEmpty(message = "权限 ID 列表不能为空")
    private List<String> permissionIds;
}
