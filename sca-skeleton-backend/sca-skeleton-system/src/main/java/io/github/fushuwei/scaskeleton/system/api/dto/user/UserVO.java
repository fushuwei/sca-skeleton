package io.github.fushuwei.scaskeleton.system.api.dto.user;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户视图对象（排除密码等敏感字段）。
 *
 * @author Fu Wei
 */
@Data
public class UserVO {

    private String id;
    private String tenantId;
    private String username;
    private String nickname;
    private String realName;
    private String gender;
    private String avatar;
    private String phone;
    private String email;
    private String userCategory;
    private String userType;
    private String status;
    private LocalDateTime statusTime;
    private String statusReason;
    private Integer loginFailCount;
    private Integer mustChangePassword;
    private LocalDateTime passwordUpdateTime;
    private LocalDateTime effectiveStartTime;
    private LocalDateTime effectiveEndTime;
    private String lastLoginIp;
    private LocalDateTime lastLoginTime;
    private Integer isBuiltin;
    private String sourceType;
    private String remark;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;
    private String updateBy;
}
