package io.github.fushuwei.scaskeleton.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.dict.DictDataCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dict.DictDataPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dict.DictDataStatusRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dict.DictDataUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.dict.DictDataResponse;

import java.util.List;

/**
 * 字典数据管理 Service
 *
 * @author Fu Wei
 */
public interface SysDictDataService {

    /**
     * 分页查询字典数据列表
     *
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<DictDataResponse> pageDictData(DictDataPageRequest request);

    /**
     * 根据 ID 查询字典数据详情
     *
     * @param id 字典数据 ID
     * @return 字典数据详情
     */
    DictDataResponse getDictDataById(String id);

    /**
     * 新增字典数据
     *
     * @param request 字典数据信息
     */
    void createDictData(DictDataCreateRequest request);

    /**
     * 编辑字典数据
     *
     * @param request 字典数据信息
     */
    void updateDictData(DictDataUpdateRequest request);

    /**
     * 启用/禁用字典数据
     *
     * @param request 字典数据 ID 与目标状态
     */
    void updateDictDataStatus(DictDataStatusRequest request);

    /**
     * 删除字典数据
     *
     * @param id 字典数据 ID
     */
    void deleteDictData(String id);

    /**
     * 批量删除字典数据
     *
     * @param ids 字典数据 ID 列表
     */
    void batchDeleteDictData(List<String> ids);
}
