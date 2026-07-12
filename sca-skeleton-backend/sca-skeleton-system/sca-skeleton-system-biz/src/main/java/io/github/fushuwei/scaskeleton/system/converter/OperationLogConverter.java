package io.github.fushuwei.scaskeleton.system.converter;

import io.github.fushuwei.scaskeleton.logging.entity.SysOperationLog;
import io.github.fushuwei.scaskeleton.system.api.response.operationlog.OperationLogResponse;
import org.mapstruct.Mapper;

/**
 * 操作日志 Entity → Response 转换器。
 *
 * @author Fu Wei
 */
@Mapper(componentModel = "spring")
public interface OperationLogConverter {

    /**
     * SysOperationLog → OperationLogResponse
     */
    OperationLogResponse toOperationLogResponse(SysOperationLog log);
}
