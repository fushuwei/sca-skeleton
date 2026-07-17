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
        operationLog.setTraceId(event.getTraceId());
        operationLog.setTenantId(event.getTenantId());
        operationLog.setUserId(event.getUserId());
        operationLog.setModule(event.getModule());
        operationLog.setAction(event.getAction());
        operationLog.setHttpMethod(event.getHttpMethod());
        operationLog.setRequestUri(event.getRequestUri());
        operationLog.setClassName(event.getClassName());
        operationLog.setMethodName(event.getMethodName());
        operationLog.setRequestArgs(event.getRequestArgs());
        operationLog.setResponseResult(event.getResponseResult());
        operationLog.setIsSuccess(event.getIsSuccess());
        operationLog.setErrorMessage(event.getErrorMessage());
        operationLog.setCostMs(event.getCostMs());
        operationLog.setClientIp(event.getClientIp());
        operationLog.setLocation(event.getLocation());
        operationLog.setDevice(event.getDevice());
        operationLog.setBrowser(event.getBrowser());
        operationLog.setOs(event.getOs());
        operationLog.setOperationTime(event.getOperationTime());
        operationLogMapper.insert(operationLog);
    }
}
