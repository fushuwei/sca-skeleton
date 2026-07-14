package io.github.fushuwei.scaskeleton.system.api.request.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建角色请求对象
 *
 * @author Fu Wei
 */
@Data
public class RoleCreateRequest {

    // ==================== 基本信息 ====================

    /** 角色名称 */
    @NotBlank(message = "角色名称不能为空")
    private String name;

    /** 角色编码 */
    @NotBlank(message = "角色编码不能为空")
    private String code;

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
}
