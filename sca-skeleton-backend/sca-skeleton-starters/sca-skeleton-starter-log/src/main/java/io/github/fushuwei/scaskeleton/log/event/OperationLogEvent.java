package io.github.fushuwei.scaskeleton.log.event;

import io.github.fushuwei.scaskeleton.log.model.OperationLogRecord;
import org.springframework.context.ApplicationEvent;

import java.io.Serial;

/**
 * 操作日志事件
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
}
