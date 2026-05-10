package io.github.fushuwei.sca.system.api.dto.dept;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建 / 更新部门请求 DTO。
 *
 * @author Fu Wei
 */
@Data
public class DeptSaveRequest {

    private String id;

    @NotBlank(message = "上级部门ID不能为空")
    private String parentId;

    @NotBlank(message = "部门名称不能为空")
    private String name;

    @NotBlank(message = "部门编码不能为空")
    private String code;

    private Integer sort;
    private String leader;
    private String phone;
    private String email;
    private String status;
}
