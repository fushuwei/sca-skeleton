package io.github.fushuwei.scaskeleton.system.converter;

import io.github.fushuwei.scaskeleton.system.api.response.config.ConfigResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysConfig;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 系统配置对象转换器（MapStruct）
 *
 * @author Fu Wei
 */
@Mapper(componentModel = "spring")
public interface ConfigConverter {

    /**
     * 配置实体 → 配置响应
     *
     * @param config 配置实体
     * @return 配置响应对象
     */
    ConfigResponse toConfigResponse(SysConfig config);

    /**
     * 配置实体列表 → 配置响应列表
     *
     * @param configs 配置实体列表
     * @return 配置响应列表
     */
    List<ConfigResponse> toConfigResponseList(List<SysConfig> configs);
}
