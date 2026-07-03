package io.github.fushuwei.scaskeleton.system.converter;

import io.github.fushuwei.scaskeleton.system.api.response.permission.PermissionResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysPermission;
import org.mapstruct.Mapper;

/**
 * 权限 Entity → Response 转换器。
 *
 * @author Fu Wei
 */
@Mapper(componentModel = "spring")
public interface PermissionConverter {

    /** SysPermission → PermissionResponse */
    PermissionResponse toResponse(SysPermission permission);
}
