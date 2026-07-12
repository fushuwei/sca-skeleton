package io.github.fushuwei.scaskeleton.logging.event;

import io.github.fushuwei.scaskeleton.logging.model.OperationLogRecord;
import org.springframework.context.ApplicationEvent;

import java.io.Serial;

/**
 * 操作日志事件。
 * <p>
 * 由 {@code OperationLogAspect} 在 finally 块中发布，监听器异步消费后写入数据库。
 * 将来升级 MQ 时，本事件直接序列化为消息体，切面零改动。
 *
 * @author Fu Wei
 */
public class OperationLogEvent extends ApplicationEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    public OperationLogEvent(OperationLogRecord record) {
        super(record);
    }

    @Override
    public OperationLogRecord getSource() {
        return (OperationLogRecord) super.getSource();
    }

    public OperationLogRecord getRecord() {
        return getSource();
    }
}
