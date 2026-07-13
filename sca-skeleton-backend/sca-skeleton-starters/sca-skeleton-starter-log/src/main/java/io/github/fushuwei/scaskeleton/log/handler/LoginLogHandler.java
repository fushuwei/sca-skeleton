package io.github.fushuwei.scaskeleton.log.handler;

import io.github.fushuwei.scaskeleton.log.event.LoginLogEvent;

/**
 * 登录日志处理器
 *
 * @author Fu Wei
 */
public interface LoginLogHandler {

    /**
     * 处理登录日志记录
     *
     * @param event 业务层采集到的完整登录日志事件
     */
    void handle(LoginLogEvent event);
}
