package io.github.fushuwei.scaskeleton.system.converter;

import io.github.fushuwei.scaskeleton.system.api.response.role.RoleOptionResponse;
import io.github.fushuwei.scaskeleton.system.api.response.role.RoleResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysRole;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 角色对象转换器（MapStruct）
 *
 * @author Fu Wei
 */
@Mapper(componentModel = "spring")
public interface RoleConverter {

    /**
     * 角色实体 → 角色响应
     *
     * @param role 角色实体
     * @return 角色响应对象
     */
    RoleResponse toRoleResponse(SysRole role);

    /**
     * 角色实体列表 → 角色响应列表
     *
     * @param roles 角色实体列表
     * @return 角色响应列表
     */
    List<RoleResponse> toRoleResponseList(List<SysRole> roles);

    /**
     * 角色实体 → 角色选项响应
     *
     * @param role 角色实体
     * @return 角色选项响应对象
     */
    RoleOptionResponse toRoleOptionResponse(SysRole role);

    /**
     * 角色实体列表 → 角色选项响应列表
     *
     * @param roles 角色实体列表
     * @return 角色选项响应列表
     */
    List<RoleOptionResponse> toRoleOptionResponseList(List<SysRole> roles);
}
