package io.github.fushuwei.scaskeleton.system.api.request.dept;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新部门请求对象
 *
 * @author Fu Wei
 */
@Data
public class DeptUpdateRequest {

    /** 部门 ID */
    @NotBlank(message = "部门 ID 不能为空")
    private String id;

    /** 上级部门 ID（顶级为 "0"） */
    private String parentId;

    /** 部门名称 */
    @NotBlank(message = "部门名称不能为空")
    private String name;

    /** 部门编码 */
    @NotBlank(message = "部门编码不能为空")
    private String code;

    /** 排序号 */
    private Integer sort;

    /** 负责人 */
    private String leader;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 状态 */
    private String status;
}
