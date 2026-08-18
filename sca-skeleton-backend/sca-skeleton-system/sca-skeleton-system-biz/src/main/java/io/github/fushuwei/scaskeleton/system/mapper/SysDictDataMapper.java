package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.dict.DictDataPageRequest;
import io.github.fushuwei.scaskeleton.system.api.response.dict.DictDataResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysDictData;
import org.apache.ibatis.annotations.Param;

/**
 * 字典数据管理 Mapper
 *
 * @author Fu Wei
 */
public interface SysDictDataMapper extends BaseMapper<SysDictData> {

    /**
     * 分页查询字典数据列表
     *
     * @param page     分页对象
     * @param tenantId 租户 ID
     * @param request  查询条件
     * @return 分页结果
     */
    IPage<DictDataResponse> selectDictDataPage(IPage<DictDataResponse> page, @Param("tenantId") String tenantId, @Param("request") DictDataPageRequest request);
}
