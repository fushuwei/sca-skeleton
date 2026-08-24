package io.github.fushuwei.scaskeleton.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 通知公告实体类
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice")
public class SysNotice extends BaseEntity {

    // ==================== 基本信息 ====================

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

    // ==================== 发布信息 ====================

    /** 发布人 */
    private String publisher;

    /** 发布时间（NULL 表示未发布） */
    private LocalDateTime publishTime;

    /** 生效时间（NULL 表示立即生效） */
    private LocalDateTime effectiveTime;

    /** 失效时间（NULL 表示永久有效） */
    private LocalDateTime expireTime;

    // ==================== 展示控制 ====================

    /** 是否置顶（0否 1是） */
    private Integer isTop;

    /** 置顶到期时间（NULL 表示永久置顶） */
    private LocalDateTime topExpireTime;

    /** 是否登录弹窗提示（0否 1是） */
    private Integer isPopup;

    // ==================== 接收范围 ====================

    /** 接收范围（all 全体用户，dept 指定部门，role 指定角色，user 指定用户） */
    private String targetType;

    // ==================== 统计与排序 ====================

    /** 已读次数 */
    private Integer readCount;

    /** 排序（只用于针对置顶消息排序，非置顶消息根据发布时间降序） */
    private Integer sort;

    /** 备注 */
    private String remark;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    // ==================== 接收目标列表（非数据库字段） ====================

    /**
     * 接收目标列表（当 targetType 不为 all 时使用）
     * <p>
     * 仅用于新增/编辑时接收前端传入的目标 ID 列表，不映射数据库字段。
     * 前端传入格式：[{ targetType: "dept", targetIds: ["1", "2"] }, ...]
     * 这里简化为 List<SysNoticeTarget>，由 Service 层处理。
     */
    @TableField(exist = false)
    private java.util.List<SysNoticeTarget> targets;
}
