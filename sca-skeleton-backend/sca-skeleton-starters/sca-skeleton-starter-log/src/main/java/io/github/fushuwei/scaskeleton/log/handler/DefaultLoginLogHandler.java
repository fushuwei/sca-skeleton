package io.github.fushuwei.scaskeleton.log.handler;

import io.github.fushuwei.scaskeleton.log.entity.SysLoginLog;
import io.github.fushuwei.scaskeleton.log.event.LoginLogEvent;
import io.github.fushuwei.scaskeleton.log.mapper.SysLoginLogMapper;
import lombok.RequiredArgsConstructor;

/**
 * 默认登录日志处理器
 *
 * @author Fu Wei
 */
@RequiredArgsConstructor
public class DefaultLoginLogHandler implements LoginLogHandler {

    private final SysLoginLogMapper loginLogMapper;

    @Override
    public void handle(LoginLogEvent event) {
        SysLoginLog loginLog = new SysLoginLog();
        loginLog.setTenantId(event.getTenantId());
        loginLog.setUserId(event.getUserId());
        loginLog.setUsername(event.getUsername());
        loginLog.setClientIp(event.getClientIp());
        loginLog.setLocation(event.getLocation());
        loginLog.setDevice(event.getDevice());
        loginLog.setBrowser(event.getBrowser());
        loginLog.setOs(event.getOs());
        loginLog.setIsSuccess(event.getIsSuccess());
        loginLog.setErrorMessage(event.getErrorMessage());
        loginLog.setCostMs(event.getCostMs());
        loginLog.setLoginTime(event.getLoginTime());
        loginLogMapper.insert(loginLog);
    }
}
