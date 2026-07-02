package io.github.fushuwei.scaskeleton.system.api.dto.role;

import io.github.fushuwei.scaskeleton.core.validation.ValidGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建 / 更新角色请求 DTO。
 *
 * @author Fu Wei
 */
@Data
public class RoleSaveRequest {

    /** 角色 ID，更新时必填，创建时为空 */
    @NotBlank(groups = ValidGroup.Update.class, message = "角色ID不能为空")
    private String id;

    /** 角色名称 */
    @NotBlank(groups = {ValidGroup.Create.class, ValidGroup.Update.class}, message = "角色名称不能为空")
    private String name;

    /** 角色编码 */
    @NotBlank(groups = ValidGroup.Create.class, message = "角色编码不能为空")
    private String code;

    /**
     * 数据权限范围：
     * all-全部，tenant-租户，dept_and_sub-本部门及下级，dept-仅本部门，
     * personal-仅本人，custom-自定义
     */
    @NotNull(groups = {ValidGroup.Create.class, ValidGroup.Update.class}, message = "数据权限范围不能为空")
    private String dataScope;

    /** 排序号，越小越靠前 */
    private Integer sort;
    /** 备注 */
    private String remark;

    /** 权限 ID 列表（全量替换），创建/编辑时一并提交 */
    private List<String> permissionIds;
}
