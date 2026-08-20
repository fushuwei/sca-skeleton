package io.github.fushuwei.scaskeleton.system.api.response.notice;

import lombok.Data;

/**
 * 通知公告接收目标响应对象
 *
 * @author Fu Wei
 */
@Data
public class NoticeTargetResponse {

    /** 目标 ID */
    private String id;

    /** 通知公告 ID */
    private String noticeId;

    /** 目标类型（dept 部门，role 角色，user 用户） */
    private String targetType;

    /** 目标 ID（部门ID / 角色ID / 用户ID） */
    private String targetId;

    /** 目标名称（部门名称 / 角色名称 / 用户昵称，关联查询所得） */
    private String targetName;
}
