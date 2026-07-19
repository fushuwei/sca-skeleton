package io.github.fushuwei.scaskeleton.system.api.request.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 更新角色请求对象
 *
 * @author Fu Wei
 */
@Data
public class RoleUpdateRequest {

    // ==================== 基本信息 ====================

    /** 角色 ID */
    @NotBlank(message = "角色 ID 不能为空")
    private String id;

    /** 角色名称 */
    @NotBlank(message = "角色名称不能为空")
    private String name;

    /** 数据权限范围 */
    @NotNull(message = "数据权限范围不能为空")
    private String dataScope;

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
