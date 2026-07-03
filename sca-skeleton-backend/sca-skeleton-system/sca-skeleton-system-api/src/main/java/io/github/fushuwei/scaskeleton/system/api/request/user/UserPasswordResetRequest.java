package io.github.fushuwei.scaskeleton.system.api.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户密码重置请求对象。
 *
 * @author Fu Wei
 */
@Data
public class UserPasswordResetRequest {

    /** 新密码 */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
