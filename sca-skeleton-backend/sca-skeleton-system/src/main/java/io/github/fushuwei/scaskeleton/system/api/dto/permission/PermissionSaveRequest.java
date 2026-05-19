package io.github.fushuwei.scaskeleton.system.api.dto.permission;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建 / 更新权限请求 DTO。
 *
 * @author Fu Wei
 */
@Data
public class PermissionSaveRequest {

    private String id;

    @NotBlank(message = "父节点ID不能为空")
    private String parentId;

    @NotBlank(message = "权限名称不能为空")
    private String name;

    @NotBlank(message = "权限类型不能为空")
    private String type;

    private String code;
    private String path;
    private String component;
    private String icon;
    private Integer sort;
    private Integer isVisible;
    private Integer isExternal;
    private String status;
    private String remark;
}
