package io.github.fushuwei.scaskeleton.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.dict.DictCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dict.DictPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dict.DictStatusRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dict.DictUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.dict.DictResponse;

import java.util.List;

/**
 * 字典管理 Service
 *
 * @author Fu Wei
 */
public interface SysDictService {

    /**
     * 查询字典列表
     *
     * @return 字典列表
     */
    List<DictResponse> listDicts();

    /**
     * 分页查询字典列表
     *
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<DictResponse> pageDicts(DictPageRequest request);

    /**
     * 根据 ID 查询字典详情
     *
     * @param id 字典 ID
     * @return 字典详情
     */
    DictResponse getDictById(String id);

    /**
     * 新增字典
     *
     * @param request 字典信息
     */
    void createDict(DictCreateRequest request);

    /**
     * 编辑字典
     *
     * @param request 字典信息
     */
    void updateDict(DictUpdateRequest request);

    /**
     * 启用/禁用字典
     *
     * @param request 字典 ID 与目标状态
     */
    void updateDictStatus(DictStatusRequest request);

    /**
     * 删除字典
     *
     * @param id 字典 ID
     */
    void deleteDict(String id);

    /**
     * 批量删除字典
     *
     * @param ids 字典 ID 列表
     */
    void batchDeleteDicts(List<String> ids);
}
