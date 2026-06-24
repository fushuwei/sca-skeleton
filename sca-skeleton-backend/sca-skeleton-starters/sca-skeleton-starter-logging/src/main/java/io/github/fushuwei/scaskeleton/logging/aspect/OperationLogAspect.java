package io.github.fushuwei.scaskeleton.logging.aspect;

import tools.jackson.databind.json.JsonMapper;
import io.github.fushuwei.scaskeleton.core.trace.TraceContext;
import io.github.fushuwei.scaskeleton.core.user.CurrentUserProvider;
import io.github.fushuwei.scaskeleton.logging.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.logging.handler.OperationLogHandler;
import io.github.fushuwei.scaskeleton.logging.model.OperationLogRecord;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.Nullable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 操作日志切面。
 * <p>
 * 拦截所有标注了 {@link OperationLog} 的方法，采集操作上下文（用户、IP、请求路径、耗时、结果），
 * 填充 {@link OperationLogRecord} 后交由 {@link OperationLogHandler} 处理。
 * 若无自定义 Handler Bean，默认将日志内容以 JSON 格式打印到 SLF4J。
 *
 * @author Fu Wei
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class OperationLogAspect {

    // 注入 Jackson，用于将方法参数与返回值序列化为 JSON 字符串
    private final JsonMapper jsonMapper;

    // 可选注入：当前用户信息提供者，未提供时操作人字段留空
    @Nullable
    private final CurrentUserProvider currentUserProvider;

    // 可选注入：业务服务提供的日志处理实现，未提供时回退到 SLF4J 打印
    @Nullable
    private final OperationLogHandler operationLogHandler;

    /**
     * 环绕通知：拦截所有标注 @OperationLog 的方法，采集完整操作上下文。
     *
     * @param joinPoint  切入点，用于获取方法签名和参数
     * @param annotation 方法上的 @OperationLog 注解，包含 module、action 等元数据
     * @return 原方法的返回值，异常时向上透传
     */
    @Around("@annotation(annotation)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog annotation) throws Throwable {
        // 记录方法开始执行时间，用于计算耗时
        long startTime = System.currentTimeMillis();

        OperationLogRecord record = new OperationLogRecord();
        // 从注解中读取业务语义描述
        record.setModule(annotation.module());
        record.setAction(annotation.action());
        // 从 TraceContext 获取当前链路 ID
        record.setTraceId(TraceContext.get());
        // 记录操作发生时间
        record.setOperationTime(LocalDateTime.now());

        // 从方法签名中提取目标类名与方法名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        record.setClassName(joinPoint.getTarget().getClass().getName());
        record.setMethodName(signature.getName());

        // 填充当前用户信息（依赖 Security Starter 的 CurrentUserProvider 实现）
        if (currentUserProvider != null) {
            record.setUserId(currentUserProvider.getUserId());
            record.setUsername(currentUserProvider.getUsername());
        }

        // 从 Spring Web 请求上下文中提取 HTTP 信息
        fillHttpContext(record);

        // 如果注解配置了记录请求参数，将方法入参序列化为 JSON
        if (annotation.logArgs()) {
            try {
                record.setRequestArgs(jsonMapper.writeValueAsString(joinPoint.getArgs()));
            } catch (Exception e) {
                // 序列化失败不影响业务，仅记录序列化错误标识
                record.setRequestArgs("[serialize error]");
            }
        }

        Object result = null;
        try {
            // 执行原方法，捕获返回值
            result = joinPoint.proceed();
            record.setSuccess(true);
            // 如果注解配置了记录响应结果，将返回值序列化为 JSON
            if (annotation.logResult() && result != null) {
                try {
                    record.setResponseResult(jsonMapper.writeValueAsString(result));
                } catch (Exception e) {
                    record.setResponseResult("[serialize error]");
                }
            }
            return result;
        } catch (Throwable throwable) {
            // 标记操作失败并记录异常描述
            record.setSuccess(false);
            record.setErrorMessage(throwable.getMessage());
            // 异常继续向上抛出，不吞掉业务异常
            throw throwable;
        } finally {
            // 无论成功或失败，均计算耗时并触发日志处理
            record.setCostMs(System.currentTimeMillis() - startTime);
            handleLog(record);
        }
    }

    /**
     * 从 Spring Web 请求上下文中提取 HTTP 方法、请求路径和客户端 IP。
     * 非 Web 场景（如单元测试）下 RequestContextHolder 为 null，跳过填充。
     *
     * @param record 待填充的操作日志记录
     */
    private void fillHttpContext(OperationLogRecord record) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // 非 Web 请求（异步任务、定时任务）时跳过 HTTP 信息填充
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        record.setHttpMethod(request.getMethod());
        record.setRequestUri(request.getRequestURI());
        // 优先从 X-Forwarded-For 获取真实客户端 IP（经过反向代理时有效）
        record.setClientIp(resolveClientIp(request));
    }

    /**
     * 解析客户端真实 IP，依次尝试常见反向代理请求头，最终回退到 RemoteAddr。
     *
     * @param request HTTP 请求
     * @return 客户端 IP 字符串
     */
    private String resolveClientIp(HttpServletRequest request) {
        // 依次尝试各反向代理透传的真实 IP 请求头
        String[] headerNames = {
                "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"
        };
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For 可能包含多个 IP，取第一个（最近的真实客户端）
                return ip.split(",")[0].trim();
            }
        }
        // 无反向代理时直接返回 TCP 连接的远端地址
        return request.getRemoteAddr();
    }

    /**
     * 将操作日志分发给 Handler 或回退到 SLF4J 打印。
     *
     * @param record 完整的操作日志记录
     */
    private void handleLog(OperationLogRecord record) {
        if (operationLogHandler != null) {
            // 委托给业务服务提供的自定义处理实现（如写库、发 MQ）
            try {
                operationLogHandler.handle(record);
            } catch (Exception e) {
                // Handler 执行失败不影响业务，降级为日志打印
                log.warn("[OperationLog] handler failed, fallback to log. traceId={}", record.getTraceId(), e);
                logToSlf4j(record);
            }
        } else {
            // 未注册 Handler 时，默认打印到 SLF4J INFO 级别
            logToSlf4j(record);
        }
    }

    /**
     * 将操作日志以结构化文本形式打印到 SLF4J。
     *
     * @param record 操作日志记录
     */
    private void logToSlf4j(OperationLogRecord record) {
        log.info("[OperationLog] traceId={} module={} action={} user={} uri={} costMs={} success={}",
                record.getTraceId(), record.getModule(), record.getAction(),
                record.getUsername(), record.getRequestUri(),
                record.getCostMs(), record.isSuccess());
    }

    // Optional 注入支持，允许 currentUserProvider 和 operationLogHandler 不存在
    public OperationLogAspect(JsonMapper jsonMapper) {
        this(jsonMapper, null, null);
    }
}
