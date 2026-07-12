package io.github.fushuwei.scaskeleton.log.handler;

import io.github.fushuwei.scaskeleton.log.model.OperationLogRecord;

/**
 * 操作日志处理器
 *
 * @author Fu Wei
 */
public interface OperationLogHandler {

    /**
     * 处理操作日志记录
     *
     * @param record 切面收集到的完整操作日志数据
     */
    void handle(OperationLogRecord record);
}
