package io.github.fushuwei.scaskeleton.system.api.dto.role;

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

    private String id;

    @NotBlank(message = "角色名称不能为空")
    private String name;

    @NotBlank(message = "角色编码不能为空")
    private String code;

    @NotNull(message = "数据权限范围不能为空")
    private String dataScope;

    private Integer sort;
    private String remark;
}
