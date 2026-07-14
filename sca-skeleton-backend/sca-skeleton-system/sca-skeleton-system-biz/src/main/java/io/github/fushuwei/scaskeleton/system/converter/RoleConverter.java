package io.github.fushuwei.scaskeleton.system.converter;

import io.github.fushuwei.scaskeleton.system.api.response.role.RoleResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysRole;
import org.mapstruct.Mapper;

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
}
