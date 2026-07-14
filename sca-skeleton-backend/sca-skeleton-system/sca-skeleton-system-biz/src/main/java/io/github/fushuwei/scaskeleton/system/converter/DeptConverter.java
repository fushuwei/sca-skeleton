package io.github.fushuwei.scaskeleton.system.converter;

import io.github.fushuwei.scaskeleton.system.api.response.dept.DeptResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysDept;
import org.mapstruct.Mapper;

/**
 * 部门对象转换器（MapStruct）
 *
 * @author Fu Wei
 */
@Mapper(componentModel = "spring")
public interface DeptConverter {

    /**
     * 部门实体 → 部门响应
     *
     * @param dept 部门实体
     * @return 部门响应对象
     */
    DeptResponse toDeptResponse(SysDept dept);
}
