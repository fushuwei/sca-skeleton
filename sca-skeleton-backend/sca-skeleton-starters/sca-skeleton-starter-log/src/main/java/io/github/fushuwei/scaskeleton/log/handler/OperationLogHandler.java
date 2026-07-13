package io.github.fushuwei.scaskeleton.log.handler;

import io.github.fushuwei.scaskeleton.log.event.OperationLogEvent;

/**
 * 操作日志处理器
 *
 * @author Fu Wei
 */
public interface OperationLogHandler {

    /**
     * 处理操作日志记录
     *
     * @param event 切面采集到的完整操作日志事件
     */
    void handle(OperationLogEvent event);
}
