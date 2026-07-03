package io.github.fushuwei.scaskeleton.system.api.request.user;

import io.github.fushuwei.scaskeleton.core.validation.ValidGroup;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建 / 更新用户请求对象。
 *
 * @author Fu Wei
 */
@Data
public class UserSaveRequest {

    /** 用户ID（更新时必传） */
    @NotBlank(groups = ValidGroup.Update.class, message = "用户ID不能为空")
    private String id;

    @NotBlank(groups = ValidGroup.Create.class, message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度 2-50 个字符")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    /** 创建时可设初始密码；更新时如不传则不修改 */
    @Size(min = 8, max = 20, message = "密码长度 8-20 个字符")
    private String password;

    private String nickname;
    private String realName;

    @Pattern(regexp = "^(male|female|other)$", message = "性别只能是 male、female 或 other")
    private String gender;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "用户类型不能为空")
    private String userType;

    @Pattern(regexp = "^(active|inactive|locked|frozen|expired|disabled|cancelled)$",
            message = "状态值不合法")
    private String status;

    /** 部门ID列表（第一个为主部门） */
    private List<String> deptIds;

    /** 岗位ID列表 */
    private List<String> postIds;

    /** 角色ID列表 */
    private List<String> roleIds;

    private LocalDateTime effectiveStartTime;
    private LocalDateTime effectiveEndTime;
    private String remark;
}
