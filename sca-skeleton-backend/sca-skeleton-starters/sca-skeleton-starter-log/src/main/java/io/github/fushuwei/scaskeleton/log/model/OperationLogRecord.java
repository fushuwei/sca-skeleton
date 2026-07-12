package io.github.fushuwei.scaskeleton.log.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志记录模型
 *
 * @author Fu Wei
 */
@Data
public class OperationLogRecord {

    /**
     * 链路追踪 ID
     */
    private String traceId;

    /**
     * 操作模块
     */
    private String module;

    /**
     * 操作动作
     */
    private String action;

    /**
     * 操作人 ID
     */
    private String userId;

    /**
     * 操作人用户名
     */
    private String username;

    /**
     * 操作人 IP
     */
    private String clientIp;

    /**
     * 请求方法（GET/POST/PUT 等）
     */
    private String httpMethod;

    /**
     * 请求路径
     */
    private String requestUri;

    /**
     * 目标类全限定名
     */
    private String className;

    /**
     * 目标方法名
     */
    private String methodName;

    /**
     * 请求参数（JSON 序列化，logArgs=false 时为 null）
     */
    private String requestArgs;

    /**
     * 响应结果（JSON 序列化，logResult=false 时为 null）
     */
    private String responseResult;

    /**
     * 是否成功：true-成功，false-异常
     */
    private boolean isSuccess;

    /**
     * 异常信息（success=false 时填充）
     */
    private String errorMessage;

    /**
     * 操作耗时（毫秒）
     */
    private long costMs;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;
}
