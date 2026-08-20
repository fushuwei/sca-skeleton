package io.github.fushuwei.scaskeleton.system.api.request.notice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 通知公告状态变更请求对象（发布、撤回、归档）
 *
 * @author Fu Wei
 */
@Data
public class NoticeStatusRequest {

    /** 通知公告 ID */
    @NotBlank(message = "通知公告 ID 不能为空")
    private String id;

    /** 目标状态（published 已发布，revoked 已撤回，archived 已归档） */
    @NotBlank(message = "目标状态不能为空")
    @Pattern(regexp = "^(published|revoked|archived)$", message = "状态值必须为 published、revoked 或 archived")
    private String status;
}
