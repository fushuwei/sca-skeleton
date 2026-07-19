package io.github.fushuwei.scaskeleton.system.api.request.post;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新岗位请求对象
 *
 * @author Fu Wei
 */
@Data
public class PostUpdateRequest {

    /** 岗位 ID */
    @NotBlank(message = "岗位 ID 不能为空")
    private String id;

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

    // ==================== 乐观锁 ====================

    /** 乐观锁版本号 */
    private Integer version;
}
