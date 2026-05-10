package io.github.fushuwei.sca.system.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.fushuwei.sca.starter.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户实体（完整版）。
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String tenantId;
    private String username;
    private String password;
    private String nickname;
    private String realName;
    private String gender;
    private String avatar;
    private String phone;
    private String email;
    /** 用户类别：backend / frontend */
    private String userCategory;
    /** 用户类型：superadmin / tenant_admin / dept_admin / normal */
    private String userType;
    /** 状态：active / inactive / locked / frozen / expired / disabled / cancelled */
    private String status;
    private LocalDateTime statusTime;
    private String statusReason;
    private Integer loginFailCount;
    /** 是否必须修改密码：0-否，1-是 */
    private Integer mustChangePassword;
    private LocalDateTime passwordUpdateTime;
    private LocalDateTime effectiveStartTime;
    private LocalDateTime effectiveEndTime;
    private String lastLoginIp;
    private LocalDateTime lastLoginTime;
    /** 是否系统内置：0-否，1-是 */
    private Integer isBuiltin;
    /** 数据来源：initial / manual / import / sync / sso */
    private String sourceType;
    private String remark;

    @Version
    private Integer version;
}
