package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.dict.DictPageRequest;
import io.github.fushuwei.scaskeleton.system.api.response.dict.DictResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysDict;
import org.apache.ibatis.annotations.Param;

/**
 * 字典管理 Mapper
 *
 * @author Fu Wei
 */
public interface SysDictMapper extends BaseMapper<SysDict> {

    /**
     * 分页查询字典列表
     *
     * @param page     分页对象
     * @param tenantId 租户 ID
     * @param request  查询条件
     * @return 分页结果
     */
    IPage<DictResponse> selectDictPage(IPage<DictResponse> page, @Param("tenantId") String tenantId, @Param("request") DictPageRequest request);
}
