package io.github.fushuwei.scaskeleton.logging.handler;

import io.github.fushuwei.scaskeleton.logging.entity.SysOperationLog;
import io.github.fushuwei.scaskeleton.logging.mapper.SysOperationLogMapper;
import io.github.fushuwei.scaskeleton.logging.model.OperationLogRecord;
import lombok.RequiredArgsConstructor;

/**
 * 操作日志默认持久化处理器：将 {@link OperationLogRecord} 写入 {@code sys_operation_log} 表。
 * <p>
 * 由 {@code LoggingAutoConfiguration} 在无自定义 Handler 时自动装配。
 * 本方法不捕获异常——持久化失败时异常向上传播至 {@code OperationLogEventListener}，
 * 由监听器统一降级为 SLF4J 打印，确保降级路径可达且职责单一。
 *
 * @author Fu Wei
 */
@RequiredArgsConstructor
public class DbOperationLogHandler implements OperationLogHandler {

    private final SysOperationLogMapper operationLogMapper;

    @Override
    public void handle(OperationLogRecord record) {
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
    }
}
