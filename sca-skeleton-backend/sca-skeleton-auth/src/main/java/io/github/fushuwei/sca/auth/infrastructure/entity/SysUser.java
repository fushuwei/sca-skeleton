package io.github.fushuwei.sca.auth.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.fushuwei.sca.starter.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户实体（认证服务专用版）。
 * <p>
 * 仅包含认证所需字段，完整业务字段由 system 服务管理。
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    /** 租户 ID */
    private String tenantId;

    /** 登录用户名 */
    private String username;

    /** 密码（BCrypt 加密，格式：{bcrypt}$2a$10$...） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 用户类别：backend 后台用户，frontend 前台用户 */
    private String userCategory;

    /** 用户类型：superadmin / tenant_admin / dept_admin / normal */
    private String userType;

    /**
     * 账号状态：
     * active-正常，inactive-未激活，locked-锁定，frozen-冻结，
     * expired-过期，disabled-禁用，cancelled-注销
     */
    private String status;

    /** 连续登录失败次数，失败后累加，成功后清零 */
    private Integer loginFailCount;

    /** 是否必须修改密码（1-是，新用户默认为 1） */
    private Integer mustChangePassword;

    /** 账号有效期起始时间（NULL 表示立即生效） */
    private LocalDateTime effectiveStartTime;

    /** 账号有效期结束时间（NULL 表示永不过期） */
    private LocalDateTime effectiveEndTime;

    /** 最后登录 IP */
    private String lastLoginIp;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
