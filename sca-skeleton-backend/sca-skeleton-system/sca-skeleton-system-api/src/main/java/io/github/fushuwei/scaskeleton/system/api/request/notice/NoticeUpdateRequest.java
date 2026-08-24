package io.github.fushuwei.scaskeleton.system.api.request.notice;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 更新通知公告请求对象
 *
 * @author Fu Wei
 */
@Data
public class NoticeUpdateRequest {

    /** 通知公告 ID */
    @jakarta.validation.constraints.NotBlank(message = "通知公告 ID 不能为空")
    private String id;

    /** 标题 */
    @jakarta.validation.constraints.NotBlank(message = "标题不能为空")
    private String title;

    /** 类型（notice 通知，announcement 公告，system 系统消息） */
    @jakarta.validation.constraints.NotBlank(message = "类型不能为空")
    @jakarta.validation.constraints.Pattern(regexp = "^(notice|announcement|system)$", message = "类型只能是 notice、announcement 或 system")
    private String type;

    /** 内容 */
    private String content;

    /** 重要级别（normal 普通，important 重要，urgent 紧急） */
    @jakarta.validation.constraints.NotBlank(message = "重要级别不能为空")
    private String level;

    /** 生效时间（NULL 表示立即生效） */
    private LocalDateTime effectiveTime;

    /** 失效时间（NULL 表示永久有效） */
    private LocalDateTime expireTime;

    /** 是否置顶（0否 1是） */
    private Integer isTop;

    /** 置顶到期时间（NULL 表示永久置顶） */
    private LocalDateTime topExpireTime;

    /** 是否登录弹窗提示（0否 1是） */
    private Integer isPopup;

    /** 接收范围（all 全体用户，dept 指定部门，role 指定角色，user 指定用户） */
    @jakarta.validation.constraints.NotBlank(message = "接收范围不能为空")
    private String targetType;

    /** 接收目标列表（当 targetType 不为 all 时必填） */
    private List<NoticeCreateRequest.NoticeTargetItem> targets;

    /** 排序（只用于针对置顶消息排序） */
    private Integer sort;

    /** 备注 */
    private String remark;

    /** 乐观锁版本号 */
    private Integer version;
}
