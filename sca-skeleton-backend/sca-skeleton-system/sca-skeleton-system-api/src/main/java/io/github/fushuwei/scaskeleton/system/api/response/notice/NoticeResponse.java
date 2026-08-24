package io.github.fushuwei.scaskeleton.system.api.response.notice;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知公告响应对象
 *
 * @author Fu Wei
 */
@Data
public class NoticeResponse {

    /** 通知公告 ID */
    private String id;

    /** 租户 ID */
    private String tenantId;

    /** 标题 */
    private String title;

    /** 类型（notice 通知，announcement 公告，system 系统消息） */
    private String type;

    /** 内容 */
    private String content;

    /** 重要级别（normal 普通，important 重要，urgent 紧急） */
    private String level;

    /** 状态（draft 草稿，published 已发布，revoked 已撤回，archived 已归档） */
    private String status;

    /** 发布人 ID */
    private String publisher;

    /** 发布人名称（关联 sys_user 查询，用户不存在时为空） */
    private String publisherName;

    /** 发布时间（NULL 表示未发布） */
    private LocalDateTime publishTime;

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
    private String targetType;

    /** 已读次数 */
    private Integer readCount;

    /** 排序 */
    private Integer sort;

    /** 备注 */
    private String remark;

    /** 乐观锁版本号 */
    private Integer version;

    /** 创建人 ID */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新人 ID */
    private String updateBy;

    /** 最后更新时间 */
    private LocalDateTime updateTime;

    /** 接收目标列表（详情查询时返回） */
    private List<NoticeTargetResponse> targets;
}
