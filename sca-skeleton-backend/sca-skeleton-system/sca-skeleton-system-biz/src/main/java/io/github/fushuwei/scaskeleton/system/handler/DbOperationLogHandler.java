package io.github.fushuwei.scaskeleton.system.handler;

import io.github.fushuwei.scaskeleton.logging.handler.OperationLogHandler;
import io.github.fushuwei.scaskeleton.logging.model.OperationLogRecord;
import io.github.fushuwei.scaskeleton.system.entity.SysOperationLog;
import io.github.fushuwei.scaskeleton.system.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 操作日志处理器实现：将 {@link OperationLogRecord} 持久化到 {@code sys_operation_log} 表。
 * <p>
 * 由 {@code LoggingAutoConfiguration} 自动发现并注入到 {@code OperationLogAspect} 切面。
 *
 * @author Fu Wei
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DbOperationLogHandler implements OperationLogHandler {

    private final SysOperationLogMapper operationLogMapper;

    @Override
    public void handle(OperationLogRecord record) {
        try {
            SysOperationLog entity = new SysOperationLog();
            entity.setTraceId(record.getTraceId());
            entity.setUserId(record.getUserId());
            entity.setModule(record.getModule());
            entity.setAction(record.getAction());
            entity.setHttpMethod(record.getHttpMethod());
            entity.setRequestUri(record.getRequestUri());
            entity.setClassName(record.getClassName());
            entity.setMethodName(record.getMethodName());
            entity.setRequestArgs(record.getRequestArgs());
            entity.setResponseResult(record.getResponseResult());
            entity.setSuccess(record.isSuccess());
            entity.setErrorMessage(record.getErrorMessage());
            entity.setCostMs(record.getCostMs());
            entity.setClientIp(record.getClientIp());
            entity.setOperationTime(record.getOperationTime());
            operationLogMapper.insert(entity);
        } catch (Exception e) {
            // 持久化失败不影响业务，降级为日志打印
            log.warn("[OperationLog] DB persist failed, traceId={}", record.getTraceId(), e);
        }
    }
}
