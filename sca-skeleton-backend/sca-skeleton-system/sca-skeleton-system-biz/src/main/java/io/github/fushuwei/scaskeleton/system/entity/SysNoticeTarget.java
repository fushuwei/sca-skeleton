package io.github.fushuwei.scaskeleton.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知公告接收目标实体类
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice_target")
public class SysNoticeTarget extends BaseEntity {

    /** 租户 ID */
    private String tenantId;

    /** 通知公告 ID */
    private String noticeId;

    /** 目标类型（dept 部门，role 角色，user 用户） */
    private String targetType;

    /** 目标 ID（部门ID / 角色ID / 用户ID） */
    private String targetId;
}
