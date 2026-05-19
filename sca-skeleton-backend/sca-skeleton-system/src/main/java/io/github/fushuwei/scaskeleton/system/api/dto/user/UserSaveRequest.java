package io.github.fushuwei.scaskeleton.system.api.dto.user;

import io.github.fushuwei.scaskeleton.core.validation.ValidGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建 / 更新用户请求 DTO。
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
    @Size(min = 6, max = 100, message = "密码长度 6-100 个字符")
    private String password;

    private String nickname;
    private String realName;
    private String gender;
    private String phone;
    private String email;

    @NotBlank(message = "用户类型不能为空")
    private String userType;

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
