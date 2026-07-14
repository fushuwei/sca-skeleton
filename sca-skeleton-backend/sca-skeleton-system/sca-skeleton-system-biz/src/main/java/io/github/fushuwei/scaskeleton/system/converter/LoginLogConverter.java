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
     * 登录日志实体 → 登录日志响应
     *
     * @param log 登录日志实体
     * @return 登录日志响应对象
     */
    LoginLogResponse toLoginLogResponse(SysLoginLog log);
}
