package io.github.fushuwei.scaskeleton.system.api.request.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建角色请求对象。
 *
 * @author Fu Wei
 */
@Data
public class RoleCreateRequest {

    /** 角色名称 */
    @NotBlank(message = "角色名称不能为空")
    private String name;

    /** 角色编码 */
    @NotBlank(message = "角色编码不能为空")
    private String code;

    /**
     * 数据权限范围：
     * all-全部，tenant-租户，dept_and_sub-本部门及下级，dept-仅本部门，
     * personal-仅本人，custom-自定义
     */
    @NotNull(message = "数据权限范围不能为空")
    private String dataScope;

    /** 排序号，越小越靠前 */
    private Integer sort;
    /** 备注 */
    private String remark;

    /** 权限 ID 列表（全量替换），创建时一并提交 */
    private List<String> permissionIds;
}
