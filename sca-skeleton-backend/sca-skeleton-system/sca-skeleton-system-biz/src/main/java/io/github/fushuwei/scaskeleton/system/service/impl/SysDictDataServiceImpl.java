package io.github.fushuwei.scaskeleton.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import io.github.fushuwei.scaskeleton.system.api.request.dict.DictDataCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dict.DictDataPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dict.DictDataStatusRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dict.DictDataUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.dict.DictDataResponse;
import io.github.fushuwei.scaskeleton.system.converter.DictConverter;
import io.github.fushuwei.scaskeleton.system.entity.SysDict;
import io.github.fushuwei.scaskeleton.system.entity.SysDictData;
import io.github.fushuwei.scaskeleton.system.mapper.SysDictMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysDictDataMapper;
import io.github.fushuwei.scaskeleton.system.service.SysDictDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * 字典数据管理 Service 实现类
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictDataServiceImpl implements SysDictDataService {

    private final SysDictDataMapper dictDataMapper;

    private final SysDictMapper dictMapper;

    private final DictConverter dictConverter;

    /**
     * 分页查询字典数据列表
     *
     * @param request 查询条件
     * @return 分页结果
     */
    @Override
    public IPage<DictDataResponse> pageDictData(DictDataPageRequest request) {
        Page<DictDataResponse> page = new Page<>(request.getPageNum(), request.getPageSize());
        // 数据隔离：仅查询当前租户下的字典数据
        String tenantId = SecurityUtils.getTenantId();
        return dictDataMapper.selectDictDataPage(page, tenantId, request);
    }

    /**
     * 根据 ID 查询字典数据详情
     *
     * @param id 字典数据 ID
     * @return 字典数据详情
     */
    @Override
    public DictDataResponse getDictDataById(String id) {
        // 加载字典数据实体并转换为响应对象
        return dictConverter.toDictDataResponse(loadDictDataEntity(id));
    }

    /**
     * 新增字典数据
     *
     * @param request 字典数据信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDictData(DictDataCreateRequest request) {
        // 获取当前登录用户所在租户的 ID
        String tenantId = SecurityUtils.getTenantId();

        // 校验字典存在且属于当前租户
        loadDictEntity(request.getDictId());

        // 封装字典数据实体
        SysDictData dictData = new SysDictData();
        dictData.setTenantId(tenantId);
        dictData.setDictId(request.getDictId());
        dictData.setLabel(request.getLabel());
        dictData.setValue(request.getValue());
        dictData.setStatus(request.getStatus());
        dictData.setSort(request.getSort() != null ? request.getSort() : 100);
        dictData.setRemark(request.getRemark());

        // 保存字典数据
        dictDataMapper.insert(dictData);
    }

    /**
     * 编辑字典数据
     *
     * @param request 字典数据信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDictData(DictDataUpdateRequest request) {
        // 加载字典数据实体
        SysDictData dictData = loadDictDataEntity(request.getId());

        // 更新字段
        dictData.setLabel(request.getLabel());
        dictData.setValue(request.getValue());
        dictData.setStatus(request.getStatus());
        dictData.setSort(request.getSort() != null ? request.getSort() : dictData.getSort());
        dictData.setRemark(request.getRemark());

        // 更新字典数据
        int affectedRows = dictDataMapper.updateById(dictData);
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.VERSION_CONFLICT);
        }
    }

    /**
     * 启用/禁用字典数据
     * <p>
     * 仅更新状态字段，不先查询整条记录：使用 UPDATE 语句配合租户隔离条件直接置状态，
     * 由 MyBatis-Plus 自动维护更新人/更新时间。
     *
     * @param request 字典数据 ID 与目标状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDictDataStatus(DictDataStatusRequest request) {
        // 数据隔离：仅更新当前租户下的字典数据
        LambdaUpdateWrapper<SysDictData> wrapper = new LambdaUpdateWrapper<SysDictData>()
            .eq(SysDictData::getId, request.getId())
            .eq(SysDictData::getTenantId, SecurityUtils.getTenantId())
            .set(SysDictData::getStatus, request.getStatus());
        int affectedRows = dictDataMapper.update(null, wrapper);
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "字典数据不存在或无权操作");
        }
    }

    /**
     * 删除字典数据
     *
     * @param id 字典数据 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDictData(String id) {
        // 加载字典数据实体并校验存在与租户隔离
        loadDictDataEntity(id);

        // 删除字典数据
        dictDataMapper.deleteById(id);
    }

    /**
     * 批量删除字典数据
     *
     * @param ids 字典数据 ID 列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteDictData(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        // 批量加载字典数据实体并校验存在与租户隔离
        loadDictDataEntities(ids);

        // 批量删除字典数据
        dictDataMapper.deleteBatchIds(ids);
    }

    /**
     * 根据 ID 加载字典数据实体
     *
     * @param id 字典数据 ID
     * @return 字典数据实体
     */
    private SysDictData loadDictDataEntity(String id) {
        SysDictData dictData = dictDataMapper.selectById(id);
        if (dictData == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "字典数据不存在");
        }
        // 数据隔离：仅允许操作当前租户下的字典数据
        if (!Objects.equals(dictData.getTenantId(), SecurityUtils.getTenantId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "权限不足，无法操作其他租户的数据");
        }
        return dictData;
    }

    /**
     * 根据 ID 列表批量加载字典数据实体并校验存在性与租户隔离
     *
     * @param ids 字典数据 ID 列表
     * @return 字典数据实体列表
     */
    private List<SysDictData> loadDictDataEntities(List<String> ids) {
        List<String> distinctIds = ids.stream().distinct().toList();
        List<SysDictData> entities = dictDataMapper.selectBatchIds(distinctIds);
        if (entities.size() != distinctIds.size()) {
            List<String> foundIds = entities.stream().map(SysDictData::getId).toList();
            List<String> missing = distinctIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new BusinessException(ResultCode.NOT_FOUND, "字典数据不存在，ID: " + String.join(", ", missing));
        }
        for (SysDictData entity : entities) {
            if (!Objects.equals(entity.getTenantId(), SecurityUtils.getTenantId())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "权限不足，无法操作其他租户的数据");
            }
        }
        return entities;
    }

    /**
     * 加载字典实体并校验存在与租户隔离
     *
     * @param dictId 字典 ID
     * @return 字典实体
     */
    private SysDict loadDictEntity(String dictId) {
        SysDict dict = dictMapper.selectById(dictId);
        if (dict == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "字典不存在");
        }
        if (!Objects.equals(dict.getTenantId(), SecurityUtils.getTenantId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "权限不足，无法操作其他租户的数据");
        }
        return dict;
    }
}
