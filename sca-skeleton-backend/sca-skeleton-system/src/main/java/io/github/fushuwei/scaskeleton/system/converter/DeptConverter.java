package io.github.fushuwei.scaskeleton.system.converter;

import io.github.fushuwei.scaskeleton.system.api.response.dept.DeptResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysDept;
import org.mapstruct.Mapper;

/**
 * 部门 Entity → Response 转换器。
 *
 * @author Fu Wei
 */
@Mapper(componentModel = "spring")
public interface DeptConverter {

    /** SysDept → DeptResponse */
    DeptResponse toDeptResponse(SysDept dept);
}
