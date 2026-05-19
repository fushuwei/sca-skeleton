package io.github.fushuwei.scaskeleton.logging.handler;

import io.github.fushuwei.scaskeleton.logging.model.OperationLogRecord;

/**
 * 操作日志处理扩展点。
 * <p>
 * 各业务服务通过实现此接口并注册为 Spring Bean，决定操作日志的输出方式
 * （写库、发 MQ、推送到日志平台等）。未提供实现时，切面默认仅打印到 SLF4J 日志。
 *
 * @author Fu Wei
 */
public interface OperationLogHandler {

    /**
     * 处理操作日志记录。
     *
     * @param record 切面收集到的完整操作日志数据
     */
    void handle(OperationLogRecord record);
}
