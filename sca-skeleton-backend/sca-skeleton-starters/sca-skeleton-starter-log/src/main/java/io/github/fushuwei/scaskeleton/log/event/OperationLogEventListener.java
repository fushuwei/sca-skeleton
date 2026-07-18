package io.github.fushuwei.scaskeleton.log.event;

import io.github.fushuwei.scaskeleton.core.ip.IpRegionResolver;
import io.github.fushuwei.scaskeleton.log.handler.OperationLogHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    private final IpRegionResolver ipRegionResolver;

    @Async("operationLogExecutor")
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOperationLog(OperationLogEvent event) {
        // 在异步线程内解析 IP 地理位置（不阻塞请求线程）
        if (StringUtils.hasText(event.getClientIp())) {
            event.setLocation(ipRegionResolver.resolve(event.getClientIp()));
        }

        if (operationLogHandler == null) {
            logToSlf4j(event);
            return;
        }
        try {
            operationLogHandler.handle(event);
        } catch (Exception e) {
            logToSlf4j(event);
            log.error("[操作日志] 异步持久化监听器执行失败，已回退到 Slf4j 记录日志", e);
        }
    }

    private void logToSlf4j(OperationLogEvent event) {
        log.info("[操作日志] traceId={} module={} action={} userId={} username={} clientIp={} location={} " +
                "device={} browser={} os={} httpMethod={} requestUri={} className={} methodName={} " +
                "isSuccess={} costMs={} operationTime={} requestArgs={} responseResult={} errorMessage={}",
            event.getTraceId(), event.getModule(), event.getAction(),
            event.getUserId(), event.getUsername(), event.getClientIp(),
            event.getLocation(), event.getDevice(), event.getBrowser(), event.getOs(),
            event.getHttpMethod(), event.getRequestUri(), event.getClassName(), event.getMethodName(),
            event.getIsSuccess(), event.getCostMs(), event.getOperationTime(),
            event.getRequestArgs(), event.getResponseResult(), event.getErrorMessage());
    }
}
