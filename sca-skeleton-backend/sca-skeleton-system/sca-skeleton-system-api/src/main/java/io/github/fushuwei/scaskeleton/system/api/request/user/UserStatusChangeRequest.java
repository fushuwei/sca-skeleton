package io.github.fushuwei.scaskeleton.system.api.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户状态变更请求对象。
 *
 * @author Fu Wei
 */
@Data
public class UserStatusChangeRequest {

    /** 用户 ID */
    @NotBlank(message = "用户 ID 不能为空")
    private String id;

    /** 状态：active / inactive / locked / frozen / expired / disabled / cancelled */
    @NotBlank(message = "状态不能为空")
    private String status;

    /** 变更原因 */
    private String reason;
}
