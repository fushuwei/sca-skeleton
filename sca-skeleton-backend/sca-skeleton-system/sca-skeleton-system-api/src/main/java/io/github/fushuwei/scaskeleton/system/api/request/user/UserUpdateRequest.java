package io.github.fushuwei.scaskeleton.system.api.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 更新用户请求对象。
 * <p>
 * 用户名（username）和密码（password）不通过此接口修改，故不包含在此对象中。
 *
 * @author Fu Wei
 */
@Data
public class UserUpdateRequest {

    /** 用户ID */
    @NotBlank(message = "用户ID不能为空")
    private String id;

    private String nickname;
    private String realName;

    @Pattern(regexp = "^(male|female|other)$", message = "性别只能是 male、female 或 other")
    private String gender;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    /** 是否平台超级管理员（0否 1是，默认0） */
    private Integer isSuperadmin;

    /** 是否必须修改密码：0-否，1-是 */
    private Integer mustChangePassword;

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
