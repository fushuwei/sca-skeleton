package io.github.fushuwei.scaskeleton.log.handler;

import io.github.fushuwei.scaskeleton.log.entity.SysOperationLog;
import io.github.fushuwei.scaskeleton.log.mapper.SysOperationLogMapper;
import io.github.fushuwei.scaskeleton.log.model.OperationLogRecord;
import lombok.RequiredArgsConstructor;

/**
 * 默认操作日志处理器
 *
 * @author Fu Wei
 */
@RequiredArgsConstructor
public class DefaultOperationLogHandler implements OperationLogHandler {

    private final SysOperationLogMapper operationLogMapper;

    @Override
    public void handle(OperationLogRecord record) {
        SysOperationLog operationLog = new SysOperationLog();
        operationLog.setTraceId(record.getTraceId());
        operationLog.setUserId(record.getUserId());
        operationLog.setModule(record.getModule());
        operationLog.setAction(record.getAction());
        operationLog.setHttpMethod(record.getHttpMethod());
        operationLog.setRequestUri(record.getRequestUri());
        operationLog.setClassName(record.getClassName());
        operationLog.setMethodName(record.getMethodName());
        operationLog.setRequestArgs(record.getRequestArgs());
        operationLog.setResponseResult(record.getResponseResult());
        operationLog.setIsSuccess(record.getIsSuccess());
        operationLog.setErrorMessage(record.getErrorMessage());
        operationLog.setCostMs(record.getCostMs());
        operationLog.setClientIp(record.getClientIp());
        operationLog.setOperationTime(record.getOperationTime());
        operationLogMapper.insert(operationLog);
    }
}
