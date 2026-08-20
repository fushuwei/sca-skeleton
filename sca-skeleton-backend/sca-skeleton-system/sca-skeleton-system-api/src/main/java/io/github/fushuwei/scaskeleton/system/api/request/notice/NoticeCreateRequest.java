package io.github.fushuwei.scaskeleton.system.api.request.notice;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建通知公告请求对象
 *
 * @author Fu Wei
 */
@Data
public class NoticeCreateRequest {

    /** 标题 */
    @NotBlank(message = "标题不能为空")
    private String title;

    /** 类型（notice 通知，announcement 公告，system 系统消息，other 其他） */
    @NotBlank(message = "类型不能为空")
    private String type;

    /** 内容 */
    private String content;

    /** 重要级别（normal 普通，important 重要，urgent 紧急） */
    @NotBlank(message = "重要级别不能为空")
    private String level;

    /** 状态（draft 草稿，published 已发布） */
    @NotBlank(message = "状态不能为空")
    private String status;

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
    @NotBlank(message = "接收范围不能为空")
    private String targetType;

    /** 接收目标列表（当 targetType 不为 all 时必填） */
    private List<NoticeTargetItem> targets;

    /** 排序（只用于针对置顶消息排序） */
    private Integer sort;

    /** 备注 */
    private String remark;

    /**
     * 接收目标项
     */
    @Data
    public static class NoticeTargetItem {
        /** 目标类型（dept 部门，role 角色，user 用户） */
        @NotBlank(message = "目标类型不能为空")
        private String targetType;
        /** 目标 ID（部门ID / 角色ID / 用户ID） */
        @NotBlank(message = "目标ID不能为空")
        private String targetId;
    }
}
