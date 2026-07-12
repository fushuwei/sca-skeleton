package io.github.fushuwei.scaskeleton.system.api.response.operationlog;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志响应对象。
 * <p>
 * username 和 realName 通过 user_id 关联 sys_user 表查询获取。
 *
 * @author Fu Wei
 */
@Data
public class OperationLogResponse {

    private String id;
    private String traceId;
    private String userId;
    /** 操作人展示名称（格式："real_name (username)"，由 SQL JOIN 拼接生成） */
    private String operator;
    private String module;
    private String action;
    private String httpMethod;
    private String requestUri;
    private String className;
    private String methodName;
    private String requestArgs;
    private String responseResult;
    private Integer isSuccess;
    private String errorMessage;
    private Long costMs;
    private String clientIp;
    private LocalDateTime operationTime;
}
