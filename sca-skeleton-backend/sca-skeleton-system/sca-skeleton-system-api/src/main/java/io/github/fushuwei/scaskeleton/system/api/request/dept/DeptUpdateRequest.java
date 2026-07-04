package io.github.fushuwei.scaskeleton.system.api.request.dept;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新部门请求对象。
 * <p>
 * 上级部门（parentId）创建后不可修改，故不包含在此对象中。
 *
 * @author Fu Wei
 */
@Data
public class DeptUpdateRequest {

    /** 部门 ID */
    @NotBlank(message = "部门ID不能为空")
    private String id;

    /** 部门名称 */
    @NotBlank(message = "部门名称不能为空")
    private String name;

    /** 部门编码 */
    @NotBlank(message = "部门编码不能为空")
    private String code;

    /** 排序号，越小越靠前 */
    private Integer sort;
    /** 负责人姓名 */
    private String leader;
    /** 联系电话 */
    private String phone;
    /** 联系邮箱 */
    private String email;
    /** 状态：enabled / disabled */
    private String status;
}
