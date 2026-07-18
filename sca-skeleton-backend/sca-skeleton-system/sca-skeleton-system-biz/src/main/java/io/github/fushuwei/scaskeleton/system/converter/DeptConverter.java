package io.github.fushuwei.scaskeleton.system.converter;

import io.github.fushuwei.scaskeleton.system.api.response.dept.DeptOptionResponse;
import io.github.fushuwei.scaskeleton.system.api.response.dept.DeptResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysDept;
import org.mapstruct.Mapper;

import java.util.List;

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

    /**
     * 部门实体 → 部门选项响应
     *
     * @param dept 部门实体
     * @return 部门选项响应对象
     */
    DeptOptionResponse toDeptOptionResponse(SysDept dept);

    /**
     * 部门实体列表 → 部门选项响应列表
     *
     * @param depts 部门实体列表
     * @return 部门选项响应列表
     */
    List<DeptOptionResponse> toDeptOptionResponseList(List<SysDept> depts);
}
