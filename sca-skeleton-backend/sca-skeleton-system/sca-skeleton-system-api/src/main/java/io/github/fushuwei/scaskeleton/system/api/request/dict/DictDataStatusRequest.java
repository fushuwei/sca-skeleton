package io.github.fushuwei.scaskeleton.system.api.request.dict;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 字典数据启用/禁用请求对象
 *
 * @author Fu Wei
 */
@Data
public class DictDataStatusRequest {

    /** 字典数据 ID */
    @NotBlank(message = "字典数据 ID 不能为空")
    private String id;

    /** 状态（enabled 启用，disabled 禁用） */
    @NotBlank(message = "字典数据状态不能为空")
    @Pattern(regexp = "^(enabled|disabled)$", message = "状态值必须为 enabled 或 disabled")
    private String status;
}
