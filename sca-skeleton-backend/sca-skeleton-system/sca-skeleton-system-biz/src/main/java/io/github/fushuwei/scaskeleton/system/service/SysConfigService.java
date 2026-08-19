package io.github.fushuwei.scaskeleton.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.config.ConfigCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.config.ConfigPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.config.ConfigStatusRequest;
import io.github.fushuwei.scaskeleton.system.api.request.config.ConfigUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.config.ConfigResponse;

import java.util.List;

/**
 * 系统配置管理 Service
 *
 * @author Fu Wei
 */
public interface SysConfigService {

    /**
     * 查询系统配置列表
     *
     * @return 配置列表
     */
    List<ConfigResponse> listConfigs();

    /**
     * 分页查询系统配置列表
     *
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<ConfigResponse> pageConfigs(ConfigPageRequest request);

    /**
     * 根据 ID 查询系统配置详情
     *
     * @param id 配置 ID
     * @return 配置详情
     */
    ConfigResponse getConfigById(String id);

    /**
     * 新增系统配置
     *
     * @param request 配置信息
     */
    void createConfig(ConfigCreateRequest request);

    /**
     * 编辑系统配置
     *
     * @param request 配置信息
     */
    void updateConfig(ConfigUpdateRequest request);

    /**
     * 启用/禁用系统配置
     *
     * @param request 配置 ID 与目标状态
     */
    void updateConfigStatus(ConfigStatusRequest request);

    /**
     * 删除系统配置
     *
     * @param id 配置 ID
     */
    void deleteConfig(String id);

    /**
     * 批量删除系统配置
     *
     * @param ids 配置 ID 列表
     */
    void batchDeleteConfigs(List<String> ids);
}
