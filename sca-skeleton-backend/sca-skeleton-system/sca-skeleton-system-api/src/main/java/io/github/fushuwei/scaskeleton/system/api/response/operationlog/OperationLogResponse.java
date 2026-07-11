package io.github.fushuwei.scaskeleton.system.api.response.operationlog;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志响应对象。
 *
 * @author Fu Wei
 */
@Data
public class OperationLogResponse {

    private String id;
    private String traceId;
    private String userId;
    private String username;
    private String module;
    private String action;
    private String httpMethod;
    private String requestUri;
    private String className;
    private String methodName;
    private String requestArgs;
    private String responseResult;
    private Boolean success;
    private String errorMessage;
    private Long costMs;
    private String clientIp;
    private LocalDateTime operationTime;
}
