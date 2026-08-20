package io.github.fushuwei.scaskeleton.system.api.request.notice;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 通知公告置顶/取消置顶请求对象
 *
 * @author Fu Wei
 */
@Data
public class NoticeTopRequest {

    /** 通知公告 ID */
    @NotBlank(message = "通知公告 ID 不能为空")
    private String id;

    /** 是否置顶（0 取消置顶，1 置顶） */
    @NotNull(message = "是否置顶不能为空")
    @Min(value = 0, message = "是否置顶值必须为 0 或 1")
    @Max(value = 1, message = "是否置顶值必须为 0 或 1")
    private Integer isTop;
}
