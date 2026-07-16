package io.github.fushuwei.scaskeleton.system.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 通用删除请求对象
 *
 * @author Fu Wei
 */
@Data
public class DeleteRequest {

    /** ID */
    @NotBlank(message = "ID 不能为空")
    private String id;
}
