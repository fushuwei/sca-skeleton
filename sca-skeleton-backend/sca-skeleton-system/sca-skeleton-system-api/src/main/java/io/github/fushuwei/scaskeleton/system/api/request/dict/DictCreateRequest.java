package io.github.fushuwei.scaskeleton.system.api.request.dict;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建字典请求对象
 *
 * @author Fu Wei
 */
@Data
public class DictCreateRequest {

    /** 字典名称 */
    @NotBlank(message = "字典名称不能为空")
    private String name;

    /** 字典编码 */
    @NotBlank(message = "字典编码不能为空")
    private String code;

    /** 状态（enabled 启用，disabled 禁用） */
    @NotBlank(message = "字典状态不能为空")
    private String status;

    /** 备注 */
    private String remark;
}
