package io.github.fushuwei.scaskeleton.system.api.request.post;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建岗位请求对象
 *
 * @author Fu Wei
 */
@Data
public class PostCreateRequest {

    /** 岗位名称 */
    @NotBlank(message = "岗位名称不能为空")
    private String name;

    /** 岗位编码 */
    @NotBlank(message = "岗位编码不能为空")
    private String code;

    /** 排序号 */
    private Integer sort;

    /** 备注 */
    private String remark;
}
