package io.github.fushuwei.scaskeleton.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import io.github.fushuwei.scaskeleton.system.api.request.dict.DictCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dict.DictPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dict.DictStatusRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dict.DictUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.dict.DictResponse;
import io.github.fushuwei.scaskeleton.system.converter.DictConverter;
import io.github.fushuwei.scaskeleton.system.entity.SysDict;
import io.github.fushuwei.scaskeleton.system.entity.SysDictData;
import io.github.fushuwei.scaskeleton.system.mapper.SysDictDataMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysDictMapper;
import io.github.fushuwei.scaskeleton.system.service.SysDictService;
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
 * 字典管理 Service 实现类
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictServiceImpl implements SysDictService {

    private final SysDictMapper dictMapper;

    private final SysDictDataMapper dictDataMapper;

    private final DictConverter dictConverter;

    /**
     * 查询字典列表
     *
     * @return 字典列表
     */
    @Override
    public List<DictResponse> listDicts() {
        // 数据隔离：仅查询当前租户下的字典
        List<SysDict> dicts = dictMapper.selectList(new LambdaQueryWrapper<SysDict>()
            .eq(SysDict::getTenantId, SecurityUtils.getTenantId())
            .orderByAsc(SysDict::getName));
        // 转换为响应对象列表
        return dictConverter.toDictResponseList(dicts);
    }

    /**
     * 分页查询字典列表
     *
     * @param request 查询条件
     * @return 分页结果
     */
    @Override
    public IPage<DictResponse> pageDicts(DictPageRequest request) {
        Page<DictResponse> page = new Page<>(request.getPageNum(), request.getPageSize());
        // 数据隔离：仅查询当前租户下的字典
        String tenantId = SecurityUtils.getTenantId();
        return dictMapper.selectDictPage(page, tenantId, request);
    }

    /**
     * 根据 ID 查询字典详情
     *
     * @param id 字典 ID
     * @return 字典详情
     */
    @Override
    public DictResponse getDictById(String id) {
        // 加载字典实体并转换为响应对象
        return dictConverter.toDictResponse(loadDictEntity(id));
    }

    /**
     * 新增字典
     *
     * @param request 字典信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDict(DictCreateRequest request) {
        // 获取当前登录用户所在租户的 ID
        String tenantId = SecurityUtils.getTenantId();

        // 字典编码在同一个租户内唯一
        long count = dictMapper.selectCount(new LambdaQueryWrapper<SysDict>()
            .eq(SysDict::getTenantId, tenantId)
            .eq(SysDict::getCode, request.getCode()));
        if (count > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "字典编码已存在");
        }

        // 封装字典实体
        SysDict dict = new SysDict();
        dict.setTenantId(tenantId);
        dict.setName(request.getName());
        dict.setCode(request.getCode());
        dict.setStatus(request.getStatus());
        dict.setIsBuiltin(0);
        dict.setRemark(request.getRemark());

        // 保存字典
        dictMapper.insert(dict);
    }

    /**
     * 编辑字典
     *
     * @param request 字典信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDict(DictUpdateRequest request) {
        // 加载字典实体
        SysDict dict = loadDictEntity(request.getId());

        // 内置字典不允许修改编码
        if (StringUtils.hasText(request.getCode()) && !request.getCode().equals(dict.getCode())) {
            // 字典编码在同一个租户内唯一（排除自身）
            long codeCount = dictMapper.selectCount(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getTenantId, dict.getTenantId())
                .eq(SysDict::getCode, request.getCode())
                .ne(SysDict::getId, request.getId()));
            if (codeCount > 0) {
                throw new BusinessException(ResultCode.ALREADY_EXISTS, "字典编码已存在");
            }
            dict.setCode(request.getCode());
        }

        // 更新其他字段
        dict.setName(request.getName());
        dict.setStatus(request.getStatus());
        dict.setRemark(request.getRemark());

        // 乐观锁：使用前端回传的 version 作为 WHERE 条件，若版本不匹配则影响行数为 0，说明数据已被其他用户修改
        dict.setVersion(request.getVersion());
        int affectedRows = dictMapper.updateById(dict);
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.VERSION_CONFLICT);
        }
    }

    /**
     * 启用/禁用字典
     * <p>
     * 仅更新状态字段，不先查询整条记录：先加载并校验租户隔离，再按主键直接置状态，
     * 并显式维护更新人/更新时间（entity 为 null 时 MyBatis-Plus 不会自动填充），
     * 且不触发乐观锁版本校验。
     *
     * @param request 字典 ID 与目标状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDictStatus(DictStatusRequest request) {
        // 加载字典实体并校验存在与租户隔离（NOT_FOUND / FORBIDDEN 精确区分）
        loadDictEntity(request.getId());
        // 仅按主键更新状态，并显式维护审计字段（避免丢失 update_by / update_time）
        LambdaUpdateWrapper<SysDict> wrapper = new LambdaUpdateWrapper<SysDict>()
            .eq(SysDict::getId, request.getId())
            .set(SysDict::getStatus, request.getStatus())
            .set(SysDict::getUpdateBy, SecurityUtils.getUserId())
            .set(SysDict::getUpdateTime, LocalDateTime.now());
        int affectedRows = dictMapper.update(null, wrapper);
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "字典不存在或已被删除");
        }
    }

    /**
     * 删除字典
     * <p>
     * 删除字典时级联删除该字典下的所有字典数据
     *
     * @param id 字典 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDict(String id) {
        // 加载字典实体并校验存在与租户隔离
        SysDict dict = loadDictEntity(id);

        // 级联删除字典下的所有字典数据
        dictDataMapper.delete(new LambdaQueryWrapper<SysDictData>()
            .eq(SysDictData::getDictId, id));

        // 删除字典
        dictMapper.deleteById(id);
    }

    /**
     * 批量删除字典
     * <p>
     * 删除字典时级联删除字典下的所有字典数据
     *
     * @param ids 字典 ID 列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteDicts(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        // 批量加载字典实体并校验存在与租户隔离
        loadDictEntities(ids);

        // 级联删除字典数据
        for (String id : ids) {
            dictDataMapper.delete(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictId, id));
        }

        // 批量删除字典
        dictMapper.deleteBatchIds(ids);
    }

    /**
     * 根据 ID 加载字典实体
     *
     * @param id 字典 ID
     * @return 字典实体
     */
    private SysDict loadDictEntity(String id) {
        SysDict dict = dictMapper.selectById(id);
        if (dict == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "字典不存在");
        }
        // 数据隔离：仅允许操作当前租户下的字典
        if (!Objects.equals(dict.getTenantId(), SecurityUtils.getTenantId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "权限不足，无法操作其他租户的数据");
        }
        return dict;
    }

    /**
     * 根据 ID 列表批量加载字典实体并校验存在性与租户隔离
     *
     * @param ids 字典 ID 列表
     * @return 字典实体列表
     */
    private List<SysDict> loadDictEntities(List<String> ids) {
        List<String> distinctIds = ids.stream().distinct().toList();
        List<SysDict> entities = dictMapper.selectBatchIds(distinctIds);
        if (entities.size() != distinctIds.size()) {
            List<String> foundIds = entities.stream().map(SysDict::getId).toList();
            List<String> missing = distinctIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new BusinessException(ResultCode.NOT_FOUND, "字典不存在，ID: " + String.join(", ", missing));
        }
        for (SysDict entity : entities) {
            if (!Objects.equals(entity.getTenantId(), SecurityUtils.getTenantId())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "权限不足，无法操作其他租户的数据");
            }
        }
        return entities;
    }
}
