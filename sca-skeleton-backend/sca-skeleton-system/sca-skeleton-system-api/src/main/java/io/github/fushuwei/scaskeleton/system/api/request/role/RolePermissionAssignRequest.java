package io.github.fushuwei.scaskeleton.system.api.request.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 角色权限分配请求对象。
 *
 * @author Fu Wei
 */
@Data
public class RolePermissionAssignRequest {

    /** 角色 ID */
    @NotBlank(message = "角色 ID 不能为空")
    private String id;

    /** 权限 ID 列表（全量替换） */
    @NotEmpty(message = "权限 ID 列表不能为空")
    private List<String> permissionIds;
}
