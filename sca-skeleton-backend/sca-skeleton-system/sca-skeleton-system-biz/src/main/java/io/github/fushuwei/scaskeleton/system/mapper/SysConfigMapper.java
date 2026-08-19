package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.config.ConfigPageRequest;
import io.github.fushuwei.scaskeleton.system.api.response.config.ConfigResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysConfig;
import org.apache.ibatis.annotations.Param;

/**
 * 系统配置管理 Mapper
 *
 * @author Fu Wei
 */
public interface SysConfigMapper extends BaseMapper<SysConfig> {

    /**
     * 分页查询系统配置列表
     *
     * @param page     分页对象
     * @param tenantId 租户 ID
     * @param request  查询条件
     * @return 分页结果
     */
    IPage<ConfigResponse> selectConfigPage(IPage<ConfigResponse> page, @Param("tenantId") String tenantId, @Param("request") ConfigPageRequest request);
}
