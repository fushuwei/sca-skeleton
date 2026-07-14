package io.github.fushuwei.scaskeleton.system.api.request.user;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 用户批量状态变更请求对象
 *
 * @author Fu Wei
 */
@Data
public class UserBatchStatusRequest {

    /** 用户 ID 列表 */
    @NotEmpty(message = "用户 ID 列表不能为空")
    private List<String> ids;

    /** 用户状态 */
    private String status;

    /** 变更原因 */
    private String reason;
}
