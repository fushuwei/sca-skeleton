package io.github.fushuwei.scaskeleton.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    // ==================== 基本信息 ====================

    /** 租户 ID */
    private String tenantId;

    /** 登录用户名 */
    private String username;

    /** 登录密码 */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 真实姓名 */
    private String realName;

    /** 性别 */
    private String gender;

    /** 头像地址 */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    // ==================== 类型与权限 ====================

    /** 用户域（admin：后台用户；portal：前台用户） */
    private String realm;

    /** 是否超级管理员 */
    private Integer isSuperadmin;

    // ==================== 状态信息 ====================

    /** 账号状态 */
    private String status;

    /** 状态变更时间 */
    private LocalDateTime statusTime;

    /** 状态变更原因 */
    private String statusReason;

    // ==================== 安全策略 ====================

    /** 连续登录失败次数 */
    private Integer loginFailCount;

    /** 是否必须修改密码 */
    private Integer mustChangePassword;

    /** 密码更新时间 */
    private LocalDateTime passwordUpdateTime;

    /** 账号生效起始时间（null 表示立即生效） */
    private LocalDateTime effectiveStartTime;

    /** 账号生效结束时间（null 表示永不过期） */
    private LocalDateTime effectiveEndTime;

    // ==================== 登录信息 ====================

    /** 最后登录 IP */
    private String lastLoginIp;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    // ==================== 系统属性 ====================

    /** 是否系统内置 */
    private Integer isBuiltin;

    /** 数据来源 */
    private String sourceType;

    /** 备注 */
    private String remark;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
