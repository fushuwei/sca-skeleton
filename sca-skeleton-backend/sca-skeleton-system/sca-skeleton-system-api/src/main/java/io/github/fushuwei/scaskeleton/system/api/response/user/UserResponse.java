package io.github.fushuwei.scaskeleton.system.api.response.user;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户响应对象
 *
 * @author Fu Wei
 */
@Data
public class UserResponse {

    // ==================== 基本信息 ====================

    /** 用户 ID */
    private String id;

    /** 租户 ID */
    private String tenantId;

    /** 租户名称 */
    private String tenantName;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 真实姓名 */
    private String realName;

    /** 性别 */
    private String gender;

    /** 头像 URL */
    private String avatar;

    // ==================== 联系方式 ====================

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    // ==================== 类型与权限 ====================

    /** 用户域 */
    private String realm;

    /** 用户类型 */
    private String userType;

    // ==================== 状态信息 ====================

    /** 用户状态 */
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

    /** 密码最后更新时间 */
    private LocalDateTime passwordUpdateTime;

    /** 账号生效起始时间（null 表示不限） */
    private LocalDateTime effectiveStartTime;

    /** 账号生效截止时间（null 表示不限） */
    private LocalDateTime effectiveEndTime;

    // ==================== 登录信息 ====================

    /** 最近一次登录 IP */
    private String lastLoginIp;

    /** 最近一次登录时间 */
    private LocalDateTime lastLoginTime;

    // ==================== 系统属性 ====================

    /** 是否内置 */
    private Integer isBuiltin;

    /** 数据来源 */
    private String sourceType;

    /** 备注 */
    private String remark;

    /** 乐观锁版本号 */
    private Integer version;

    // ==================== 审计字段 ====================

    /** 创建人 ID */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新人 ID */
    private String updateBy;

    /** 最后更新时间 */
    private LocalDateTime updateTime;

    // ==================== 关联信息 ====================

    /** 部门 ID 列表 */
    private List<String> deptIds;

    /** 岗位 ID 列表 */
    private List<String> postIds;

    /** 角色 ID 列表 */
    private List<String> roleIds;

    /** 部门名称列表（列表展示用，逗号分隔） */
    private String deptNames;

    /** 角色名称列表（列表展示用，逗号分隔） */
    private String roleNames;
}
