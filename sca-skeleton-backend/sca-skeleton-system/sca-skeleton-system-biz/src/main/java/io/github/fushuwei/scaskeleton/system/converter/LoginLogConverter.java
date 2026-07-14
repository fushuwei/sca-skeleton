package io.github.fushuwei.scaskeleton.system.converter;

import io.github.fushuwei.scaskeleton.log.entity.SysLoginLog;
import io.github.fushuwei.scaskeleton.system.api.response.loginlog.LoginLogResponse;
import org.mapstruct.Mapper;

/**
 * 登录日志对象转换器（MapStruct）
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
