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

    /** SysDept → DeptResponse */
    DeptResponse toDeptResponse(SysDept dept);
}
