package io.github.fushuwei.scaskeleton.system.api.response.operationlog;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志响应对象
 *
 * @author Fu Wei
 */
@Data
public class OperationLogResponse {

    // ==================== 基本信息 ====================

    /** 日志 ID */
    private String id;

    /** 链路追踪 ID */
    private String traceId;

    /** 租户 ID */
    private String tenantId;

    /** 租户名称 */
    private String tenantName;

    /** 操作人 ID */
    private String userId;

    /** 操作用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    // ==================== 操作信息 ====================

    /** 操作模块 */
    private String module;

    /** 操作动作 */
    private String action;

    // ==================== 请求信息 ====================

    /** 请求方法 */
    private String httpMethod;

    /** 请求路径 */
    private String requestUri;

    /** 目标类全限定名 */
    private String className;

    /** 目标方法名 */
    private String methodName;

    /** 请求参数 */
    private String requestArgs;

    /** 响应结果 */
    private String responseResult;

    // ==================== 结果信息 ====================

    /** 是否成功 */
    private Integer isSuccess;

    /** 异常信息 */
    private String errorMessage;

    /** 操作耗时 */
    private Long costMs;

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

    /** 操作时间 */
    private LocalDateTime operationTime;
}
