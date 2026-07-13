package io.github.fushuwei.scaskeleton.log.event;

import io.github.fushuwei.scaskeleton.log.entity.SysLoginLog;
import io.github.fushuwei.scaskeleton.log.mapper.SysLoginLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登录日志异步持久化监听器
 * <p>
 * 消费 {@link LoginLogEvent}，在独立线程 + 独立事务中将日志写入数据库，
 * 不阻塞业务（如登录）主流程。执行失败时降级为 Slf4j 日志输出。
 * <p>
 * 本类只依赖 {@link SysLoginLogMapper}（位于 starter-log 内部），不依赖任何业务模块，
 * 因此可随 starter-log 被任意服务复用。
 *
 * @author Fu Wei
 */
@Slf4j
@RequiredArgsConstructor
public class LoginLogEventListener {

    private final SysLoginLogMapper loginLogMapper;

    /**
     * 异步消费登录日志事件，将日志写入数据库。
     *
     * @param event 业务层采集并发布的登录日志事件
     */
    @Async("loginLogExecutor")
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onLoginLogEvent(LoginLogEvent event) {
        try {
            SysLoginLog loginLog = new SysLoginLog();
            loginLog.setTenantId(event.tenantId());
            loginLog.setUserId(event.userId());
            loginLog.setUsername(event.username());
            loginLog.setClientIp(event.clientIp());
            loginLog.setLocation(event.location());
            loginLog.setDevice(event.device());
            loginLog.setBrowser(event.browser());
            loginLog.setOs(event.os());
            loginLog.setIsSuccess(event.isSuccess());
            loginLog.setErrorMessage(event.errorMessage());
            loginLog.setCostMs(event.costMs());
            loginLog.setLoginTime(event.loginTime());
            loginLogMapper.insert(loginLog);
        } catch (Exception e) {
            log.warn("[登录日志] 异步持久化失败，回退到 Slf4j 记录。 isSuccess={} userId={}",
                event.isSuccess(), event.userId(), e);
        }
    }
}
