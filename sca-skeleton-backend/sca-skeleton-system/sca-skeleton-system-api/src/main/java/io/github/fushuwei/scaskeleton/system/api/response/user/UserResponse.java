package io.github.fushuwei.scaskeleton.system.api.response.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户管理响应对象（排除密码等敏感字段）。
 *
 * @author Fu Wei
 */
@Data
public class UserResponse {

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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime statusTime;

    private String statusReason;
    private Integer loginFailCount;
    private Integer mustChangePassword;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime passwordUpdateTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime effectiveStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime effectiveEndTime;

    private String lastLoginIp;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    private Integer isBuiltin;
    private String sourceType;
    private String remark;
    private Integer version;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    private String createBy;
    private String updateBy;

    /** 部门ID列表（第一个为主部门） */
    private List<String> deptIds;
    /** 岗位ID列表 */
    private List<String> postIds;
    /** 角色ID列表 */
    private List<String> roleIds;
}
