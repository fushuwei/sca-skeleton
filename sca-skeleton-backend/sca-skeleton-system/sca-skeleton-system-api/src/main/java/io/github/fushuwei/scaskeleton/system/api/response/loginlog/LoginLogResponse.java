package io.github.fushuwei.scaskeleton.system.api.response.loginlog;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志响应对象。
 * <p>
 * operator 通过 user_id 关联 sys_user 表查询获取。
 *
 * @author Fu Wei
 */
@Data
public class LoginLogResponse {

    private String id;
    private String tenantId;
    /** 租户名称（通过 tenant_id 关联 sys_tenant 查询，租户不存在时为 null） */
    private String tenantName;
    private String userId;
    /** 登录时输入的用户名（原始输入，无论用户是否存在都记录） */
    private String username;
    /** 真实姓名（通过 user_id 关联 sys_user 查询，user 不存在时为 null） */
    private String realName;
    private String clientIp;
    private String location;
    private String device;
    private String browser;
    private String os;
    /** 是否成功：0-失败，1-成功 */
    private Integer isSuccess;
    private String errorMessage;
    private Long costMs;
    private LocalDateTime loginTime;
}
