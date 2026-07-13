package io.github.fushuwei.scaskeleton.log.event;

import io.github.fushuwei.scaskeleton.log.handler.OperationLogHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 操作日志异步持久化监听器
 * <p>
 * 在独立线程 + 独立事务中将日志写入数据库，请求线程发布事件后立即返回，Handler 不存在或执行失败时降级为 Slf4j 日志输出
 *
 * @author Fu Wei
 */
@Slf4j
@RequiredArgsConstructor
public class OperationLogEventListener {

    private final OperationLogHandler operationLogHandler;

    @Async("operationLogExecutor")
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOperationLog(OperationLogEvent event) {
        if (operationLogHandler == null) {
            logToSlf4j(event);
            return;
        }
        try {
            operationLogHandler.handle(event);
        } catch (Exception e) {
            log.warn("[操作日志] 异步持久化监听器执行失败，回退到 Slf4j 记录日志", e);
            logToSlf4j(event);
        }
    }

    private void logToSlf4j(OperationLogEvent event) {
        log.info("[操作日志] traceId={} module={} action={} user={} uri={} costMs={} success={}",
            event.traceId(), event.module(), event.action(), event.username(),
            event.requestUri(), event.costMs(), event.isSuccess());
    }
}
