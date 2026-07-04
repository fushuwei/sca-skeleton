package io.github.fushuwei.scaskeleton.system.api.request.post;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新岗位请求对象。
 * <p>
 * 岗位编码（code）创建后不可修改，故不包含在此对象中。
 *
 * @author Fu Wei
 */
@Data
public class PostUpdateRequest {

    /** 岗位 ID */
    @NotBlank(message = "岗位ID不能为空")
    private String id;

    /** 岗位名称 */
    @NotBlank(message = "岗位名称不能为空")
    private String name;

    /** 排序号，越小越靠前 */
    private Integer sort;
    /** 备注 */
    private String remark;
}
