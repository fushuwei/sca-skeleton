package io.github.fushuwei.scaskeleton.system.api.request.dict;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建字典数据请求对象
 *
 * @author Fu Wei
 */
@Data
public class DictDataCreateRequest {

    /** 字典 ID */
    @NotBlank(message = "字典 ID 不能为空")
    private String dictId;

    /** 字典标签 */
    @NotBlank(message = "字典标签不能为空")
    private String label;

    /** 字典值 */
    @NotBlank(message = "字典值不能为空")
    private String value;

    /** 状态（enabled 启用，disabled 禁用） */
    @NotBlank(message = "字典数据状态不能为空")
    private String status;

    /** 排序号 */
    private Integer sort;

    /** 备注 */
    private String remark;
}
