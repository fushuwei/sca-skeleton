package io.github.fushuwei.scaskeleton.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 通知公告已读记录实体类
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice_read")
public class SysNoticeRead extends BaseEntity {

    /** 租户 ID */
    private String tenantId;

    /** 通知公告 ID */
    private String noticeId;

    /** 用户 ID */
    private String userId;

    /** 读取时间 */
    private LocalDateTime readTime;
}
