package io.github.fushuwei.scaskeleton.log.event;

import io.github.fushuwei.scaskeleton.log.handler.LoginLogHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登录日志异步持久化监听器
 * <p>
 * 在独立线程 + 独立事务中将日志写入数据库，请求线程发布事件后立即返回，Handler 不存在或执行失败时降级为 Slf4j 日志输出
 *
 * @author Fu Wei
 */
@Slf4j
@RequiredArgsConstructor
public class LoginLogEventListener {

    private final LoginLogHandler loginLogHandler;

    @Async("loginLogExecutor")
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onLoginLogEvent(LoginLogEvent event) {
        if (loginLogHandler == null) {
            logToSlf4j(event);
            return;
        }
        try {
            loginLogHandler.handle(event);
        } catch (Exception e) {
            logToSlf4j(event);
            log.error("[登录日志] 异步持久化监听器执行失败，已回退到 Slf4j 记录日志", e);
        }
    }

    private void logToSlf4j(LoginLogEvent event) {
        log.info("[登录日志] isSuccess={} tenantId={} userId={} username={} clientIp={} location={} " +
                "device={} browser={} os={} costMs={} loginTime={} errorMessage={}",
            event.isSuccess(), event.tenantId(), event.userId(), event.username(),
            event.clientIp(), event.location(), event.device(), event.browser(), event.os(),
            event.costMs(), event.loginTime(), event.errorMessage());
    }
}
