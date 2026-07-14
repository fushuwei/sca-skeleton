package io.github.fushuwei.scaskeleton.system.converter;

import io.github.fushuwei.scaskeleton.log.entity.SysOperationLog;
import io.github.fushuwei.scaskeleton.system.api.response.operationlog.OperationLogResponse;
import org.mapstruct.Mapper;

/**
 * 操作日志对象转换器（MapStruct）
 *
 * @author Fu Wei
 */
@Mapper(componentModel = "spring")
public interface OperationLogConverter {

    /**
     * 操作日志实体 → 操作日志响应
     *
     * @param log 操作日志实体
     * @return 操作日志响应对象
     */
    OperationLogResponse toOperationLogResponse(SysOperationLog log);
}
