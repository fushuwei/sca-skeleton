package io.github.fushuwei.scaskeleton.system.api.response.loginlog;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志响应对象
 *
 * @author Fu Wei
 */
@Data
public class LoginLogResponse {

    // ==================== 基本信息 ====================

    /** 日志 ID */
    private String id;

    /** 租户 ID */
    private String tenantId;

    /** 租户名称 */
    private String tenantName;

    /** 用户 ID */
    private String userId;

    /** 登录时输入的用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    // ==================== 客户端信息 ====================

    /** 客户端 IP */
    private String clientIp;

    /** 登录位置 */
    private String location;

    /** 设备类型 */
    private String device;

    /** 浏览器 */
    private String browser;

    /** 操作系统 */
    private String os;

    // ==================== 结果信息 ====================

    /** 是否成功 */
    private Integer isSuccess;

    /** 异常信息 */
    private String errorMessage;

    /** 操作耗时 */
    private Long costMs;

    /** 登录时间 */
    private LocalDateTime loginTime;
}
