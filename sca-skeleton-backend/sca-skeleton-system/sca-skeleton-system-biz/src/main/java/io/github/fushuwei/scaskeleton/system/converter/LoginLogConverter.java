package io.github.fushuwei.scaskeleton.system.converter;

import io.github.fushuwei.scaskeleton.log.entity.SysLoginLog;
import io.github.fushuwei.scaskeleton.system.api.response.loginlog.LoginLogResponse;
import org.mapstruct.Mapper;

/**
 * 登录日志 Entity → Response 转换器。
 *
 * @author Fu Wei
 */
@Mapper(componentModel = "spring")
public interface LoginLogConverter {

    /**
     * SysLoginLog → LoginLogResponse
     */
    LoginLogResponse toLoginLogResponse(SysLoginLog log);
}
