package io.github.fushuwei.scaskeleton.system.converter;

import io.github.fushuwei.scaskeleton.system.api.response.dict.DictDataResponse;
import io.github.fushuwei.scaskeleton.system.api.response.dict.DictResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysDict;
import io.github.fushuwei.scaskeleton.system.entity.SysDictData;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 字典对象转换器（MapStruct）
 *
 * @author Fu Wei
 */
@Mapper(componentModel = "spring")
public interface DictConverter {

    /**
     * 字典实体 → 字典响应
     *
     * @param dict 字典实体
     * @return 字典响应对象
     */
    DictResponse toDictResponse(SysDict dict);

    /**
     * 字典实体列表 → 字典响应列表
     *
     * @param dicts 字典实体列表
     * @return 字典响应列表
     */
    List<DictResponse> toDictResponseList(List<SysDict> dicts);

    /**
     * 字典数据实体 → 字典数据响应
     *
     * @param dictData 字典数据实体
     * @return 字典数据响应对象
     */
    DictDataResponse toDictDataResponse(SysDictData dictData);

    /**
     * 字典数据实体列表 → 字典数据响应列表
     *
     * @param dictDataList 字典数据实体列表
     * @return 字典数据响应列表
     */
    List<DictDataResponse> toDictDataResponseList(List<SysDictData> dictDataList);
}
