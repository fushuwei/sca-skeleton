package io.github.fushuwei.scaskeleton.log.handler;

import io.github.fushuwei.scaskeleton.log.entity.SysLoginLog;
import io.github.fushuwei.scaskeleton.log.event.LoginLogEvent;
import io.github.fushuwei.scaskeleton.log.mapper.SysLoginLogMapper;
import lombok.RequiredArgsConstructor;

/**
 * 默认登录日志处理器：将事件数据写入 sys_login_log 表
 *
 * @author Fu Wei
 */
@RequiredArgsConstructor
public class DefaultLoginLogHandler implements LoginLogHandler {

    private final SysLoginLogMapper loginLogMapper;

    @Override
    public void handle(LoginLogEvent event) {
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
    }
}
