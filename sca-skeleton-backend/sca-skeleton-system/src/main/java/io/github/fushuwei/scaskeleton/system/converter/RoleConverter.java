package io.github.fushuwei.scaskeleton.system.converter;

import io.github.fushuwei.scaskeleton.system.api.response.role.RoleResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysRole;
import org.mapstruct.Mapper;

/**
 * 角色 Entity → Response 转换器。
 *
 * @author Fu Wei
 */
@Mapper(componentModel = "spring")
public interface RoleConverter {

    /** SysRole → RoleResponse */
    RoleResponse toRoleResponse(SysRole role);
}
