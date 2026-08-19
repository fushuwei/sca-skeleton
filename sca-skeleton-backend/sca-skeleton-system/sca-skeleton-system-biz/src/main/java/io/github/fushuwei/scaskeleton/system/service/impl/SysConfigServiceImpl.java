package io.github.fushuwei.scaskeleton.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import io.github.fushuwei.scaskeleton.system.api.request.config.ConfigCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.config.ConfigPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.config.ConfigStatusRequest;
import io.github.fushuwei.scaskeleton.system.api.request.config.ConfigUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.config.ConfigResponse;
import io.github.fushuwei.scaskeleton.system.converter.ConfigConverter;
import io.github.fushuwei.scaskeleton.system.entity.SysConfig;
import io.github.fushuwei.scaskeleton.system.mapper.SysConfigMapper;
import io.github.fushuwei.scaskeleton.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 系统配置管理 Service 实现类
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper configMapper;

    private final ConfigConverter configConverter;

    /**
     * 查询系统配置列表
     *
     * @return 配置列表
     */
    @Override
    public List<ConfigResponse> listConfigs() {
        // 数据隔离：仅查询当前租户下的配置
        List<SysConfig> configs = configMapper.selectList(new LambdaQueryWrapper<SysConfig>()
            .eq(SysConfig::getTenantId, SecurityUtils.getTenantId())
            .orderByAsc(SysConfig::getName));
        // 转换为响应对象列表
        return configConverter.toConfigResponseList(configs);
    }

    /**
     * 分页查询系统配置列表
     *
     * @param request 查询条件
     * @return 分页结果
     */
    @Override
    public IPage<ConfigResponse> pageConfigs(ConfigPageRequest request) {
        Page<ConfigResponse> page = new Page<>(request.getPageNum(), request.getPageSize());
        // 数据隔离：仅查询当前租户下的配置
        String tenantId = SecurityUtils.getTenantId();
        return configMapper.selectConfigPage(page, tenantId, request);
    }

    /**
     * 根据 ID 查询系统配置详情
     *
     * @param id 配置 ID
     * @return 配置详情
     */
    @Override
    public ConfigResponse getConfigById(String id) {
        // 加载配置实体并转换为响应对象
        return configConverter.toConfigResponse(loadConfigEntity(id));
    }

    /**
     * 新增系统配置
     *
     * @param request 配置信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createConfig(ConfigCreateRequest request) {
        // 获取当前登录用户所在租户的 ID
        String tenantId = SecurityUtils.getTenantId();

        // 配置键在同一个租户内唯一
        long count = configMapper.selectCount(new LambdaQueryWrapper<SysConfig>()
            .eq(SysConfig::getTenantId, tenantId)
            .eq(SysConfig::getConfigKey, request.getConfigKey()));
        if (count > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "配置键已存在");
        }

        // 封装配置实体
        SysConfig config = new SysConfig();
        config.setTenantId(tenantId);
        config.setName(request.getName());
        config.setConfigKey(request.getConfigKey());
        config.setConfigValue(request.getConfigValue());
        config.setType(request.getType());
        config.setStatus(request.getStatus());
        config.setIsBuiltin(0);
        config.setRemark(request.getRemark());

        // 保存配置
        configMapper.insert(config);
    }

    /**
     * 编辑系统配置
     *
     * @param request 配置信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(ConfigUpdateRequest request) {
        // 加载配置实体
        SysConfig config = loadConfigEntity(request.getId());

        // 内置配置不允许修改配置键
        if (StringUtils.hasText(request.getConfigKey()) && !request.getConfigKey().equals(config.getConfigKey())) {
            // 配置键在同一个租户内唯一（排除自身）
            long keyCount = configMapper.selectCount(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getTenantId, config.getTenantId())
                .eq(SysConfig::getConfigKey, request.getConfigKey())
                .ne(SysConfig::getId, request.getId()));
            if (keyCount > 0) {
                throw new BusinessException(ResultCode.ALREADY_EXISTS, "配置键已存在");
            }
            config.setConfigKey(request.getConfigKey());
        }

        // 更新其他字段
        config.setName(request.getName());
        config.setConfigValue(request.getConfigValue());
        config.setType(request.getType());
        config.setStatus(request.getStatus());
        config.setRemark(request.getRemark());

        // 乐观锁：使用前端回传的 version 作为 WHERE 条件，若版本不匹配则影响行数为 0，说明数据已被其他用户修改
        config.setVersion(request.getVersion());
        int affectedRows = configMapper.updateById(config);
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.VERSION_CONFLICT);
        }
    }

    /**
     * 启用/禁用系统配置
     * <p>
     * 仅更新状态字段：先加载并校验租户隔离，再按主键直接置状态，
     * 并显式维护更新人/更新时间（entity 为 null 时 MyBatis-Plus 不会自动填充），
     * 且不触发乐观锁版本校验。
     *
     * @param request 配置 ID 与目标状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfigStatus(ConfigStatusRequest request) {
        // 加载配置实体并校验存在与租户隔离（NOT_FOUND / FORBIDDEN 精确区分）
        loadConfigEntity(request.getId());
        // 仅按主键更新状态，并显式维护审计字段（避免丢失 update_by / update_time）
        LambdaUpdateWrapper<SysConfig> wrapper = new LambdaUpdateWrapper<SysConfig>()
            .eq(SysConfig::getId, request.getId())
            .set(SysConfig::getStatus, request.getStatus())
            .set(SysConfig::getUpdateBy, SecurityUtils.getUserId())
            .set(SysConfig::getUpdateTime, LocalDateTime.now());
        int affectedRows = configMapper.update(null, wrapper);
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "系统配置不存在或已被删除");
        }
    }

    /**
     * 删除系统配置
     *
     * @param id 配置 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(String id) {
        // 加载配置实体并校验存在与租户隔离
        SysConfig config = loadConfigEntity(id);

        // 内置配置不允许删除
        if (config.getIsBuiltin() != null && config.getIsBuiltin() == 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "内置系统配置不允许删除");
        }

        // 删除配置
        configMapper.deleteById(id);
    }

    /**
     * 批量删除系统配置
     *
     * @param ids 配置 ID 列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteConfigs(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        // 批量加载配置实体并校验存在与租户隔离
        List<SysConfig> entities = loadConfigEntities(ids);

        // 内置配置不允许删除
        for (SysConfig entity : entities) {
            if (entity.getIsBuiltin() != null && entity.getIsBuiltin() == 1) {
                throw new BusinessException(ResultCode.FORBIDDEN, "内置系统配置「" + entity.getName() + "」不允许删除");
            }
        }

        // 批量删除配置
        configMapper.deleteBatchIds(ids);
    }

    /**
     * 根据 ID 加载配置实体
     *
     * @param id 配置 ID
     * @return 配置实体
     */
    private SysConfig loadConfigEntity(String id) {
        SysConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "系统配置不存在");
        }
        // 数据隔离：仅允许操作当前租户下的配置
        if (!Objects.equals(config.getTenantId(), SecurityUtils.getTenantId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "权限不足，无法操作其他租户的数据");
        }
        return config;
    }

    /**
     * 根据 ID 列表批量加载配置实体并校验存在性与租户隔离
     *
     * @param ids 配置 ID 列表
     * @return 配置实体列表
     */
    private List<SysConfig> loadConfigEntities(List<String> ids) {
        List<String> distinctIds = ids.stream().distinct().toList();
        List<SysConfig> entities = configMapper.selectBatchIds(distinctIds);
        if (entities.size() != distinctIds.size()) {
            List<String> foundIds = entities.stream().map(SysConfig::getId).toList();
            List<String> missing = distinctIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new BusinessException(ResultCode.NOT_FOUND, "系统配置不存在，ID: " + String.join(", ", missing));
        }
        for (SysConfig entity : entities) {
            if (!Objects.equals(entity.getTenantId(), SecurityUtils.getTenantId())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "权限不足，无法操作其他租户的数据");
            }
        }
        return entities;
    }
}
