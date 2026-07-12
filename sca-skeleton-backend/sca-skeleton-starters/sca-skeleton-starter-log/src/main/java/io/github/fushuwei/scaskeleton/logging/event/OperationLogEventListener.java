package io.github.fushuwei.scaskeleton.logging.event;

import io.github.fushuwei.scaskeleton.logging.handler.OperationLogHandler;
import io.github.fushuwei.scaskeleton.logging.model.OperationLogRecord;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 操作日志异步持久化监听器。
 * <p>
 * 在独立线程 + 独立事务中将日志写入数据库，请求线程发布事件后立即返回。
 * Handler 不存在时降级为 SLF4J 打印；Handler 执行失败时同样降级为 SLF4J 打印。
 *
 * @author Fu Wei
 */
@Slf4j
public class OperationLogEventListener {

    @Nullable
    private final OperationLogHandler operationLogHandler;

    public OperationLogEventListener(@Nullable OperationLogHandler operationLogHandler) {
        this.operationLogHandler = operationLogHandler;
    }

    @Async("operationLogExecutor")
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOperationLog(OperationLogEvent event) {
        OperationLogRecord record = event.getRecord();
        if (operationLogHandler == null) {
            logToSlf4j(record);
            return;
        }
        try {
            operationLogHandler.handle(record);
        } catch (Exception e) {
            log.warn("[OperationLog] async persist failed, fallback to SLF4J. traceId={}",
                    record.getTraceId(), e);
            logToSlf4j(record);
        }
    }

    private void logToSlf4j(OperationLogRecord record) {
        log.info("[OperationLog] traceId={} module={} action={} user={} uri={} costMs={} success={}",
                record.getTraceId(), record.getModule(), record.getAction(),
                record.getUsername(), record.getRequestUri(),
                record.getCostMs(), record.isSuccess());
    }
}
