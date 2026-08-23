package io.github.fushuwei.scaskeleton.system.api.response.notice;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息通知收件箱响应对象
 * <p>
 * 用于顶部消息通知铃铛弹窗展示，相比 {@link NoticeResponse} 精简了管理类字段，
 * 新增 isRead 字段标识当前用户的已读/未读状态。
 *
 * @author Fu Wei
 */
@Data
public class NoticeInboxResponse {

    /** 通知公告 ID */
    private String id;

    /** 标题 */
    private String title;

    /** 类型（notice 通知，announcement 公告，system 系统消息，other 其他） */
    private String type;

    /** 内容（纯文本摘要，前端弹窗展示用） */
    private String content;

    /** 重要级别（normal 普通，important 重要，urgent 紧急） */
    private String level;

    /** 是否置顶（0否 1是） */
    private Integer isTop;

    /** 发布人名称 */
    private String publisherName;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 当前用户是否已读（true 已读，false 未读） */
    private Boolean isRead;

    /** 读取时间（未读时为 null） */
    private LocalDateTime readTime;
}
