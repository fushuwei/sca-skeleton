package io.github.fushuwei.scaskeleton.log.event;

import java.time.LocalDateTime;

/**
 * 操作日志事件元数据载体类
 *
 * @author Fu Wei
 */
public record OperationLogEvent(
    /** 链路追踪 ID */
    String traceId,
    /** 操作模块 */
    String module,
    /** 操作动作 */
    String action,
    /** 操作人 ID */
    String userId,
    /** 操作人用户名 */
    String username,
    /** 客户端 IP */
    String clientIp,
    /** 请求方法（GET/POST/PUT 等） */
    String httpMethod,
    /** 请求路径 */
    String requestUri,
    /** 目标类全限定名 */
    String className,
    /** 目标方法名 */
    String methodName,
    /** 请求参数（JSON 序列化，logArgs=false 时为 null） */
    String requestArgs,
    /** 响应结果（JSON 序列化，logResult=false 时为 null） */
    String responseResult,
    /** 是否成功：0-失败，1-成功 */
    Integer isSuccess,
    /** 异常信息（isSuccess=0 时填充） */
    String errorMessage,
    /** 操作耗时（毫秒） */
    long costMs,
    /** 操作时间 */
    LocalDateTime operationTime
) {
}
