package io.github.fushuwei.scaskeleton.system.api.request.notice;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 通知公告标记已读请求对象
 *
 * @author Fu Wei
 */
@Data
public class NoticeReadRequest {

    /** 通知公告 ID */
    @NotBlank(message = "通知公告 ID 不能为空")
    private String id;
}
