package io.github.fushuwei.scaskeleton.log.handler;

import io.github.fushuwei.scaskeleton.log.entity.SysOperationLog;
import io.github.fushuwei.scaskeleton.log.event.OperationLogEvent;
import io.github.fushuwei.scaskeleton.log.mapper.SysOperationLogMapper;
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
    public void handle(OperationLogEvent event) {
        SysOperationLog operationLog = new SysOperationLog();
        operationLog.setTraceId(event.traceId());
        operationLog.setUserId(event.userId());
        operationLog.setModule(event.module());
        operationLog.setAction(event.action());
        operationLog.setHttpMethod(event.httpMethod());
        operationLog.setRequestUri(event.requestUri());
        operationLog.setClassName(event.className());
        operationLog.setMethodName(event.methodName());
        operationLog.setRequestArgs(event.requestArgs());
        operationLog.setResponseResult(event.responseResult());
        operationLog.setIsSuccess(event.isSuccess());
        operationLog.setErrorMessage(event.errorMessage());
        operationLog.setCostMs(event.costMs());
        operationLog.setClientIp(event.clientIp());
        operationLog.setOperationTime(event.operationTime());
        operationLogMapper.insert(operationLog);
    }
}
