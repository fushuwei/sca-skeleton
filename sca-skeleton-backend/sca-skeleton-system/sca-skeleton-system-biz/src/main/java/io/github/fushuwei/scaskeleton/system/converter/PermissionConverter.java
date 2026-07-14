package io.github.fushuwei.scaskeleton.system.converter;

import io.github.fushuwei.scaskeleton.system.api.response.permission.PermissionResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysPermission;
import org.mapstruct.Mapper;

/**
 * 权限对象转换器（MapStruct）
 *
 * @author Fu Wei
 */
@Mapper(componentModel = "spring")
public interface PermissionConverter {

    /** SysPermission → PermissionResponse */
    PermissionResponse toPermissionResponse(SysPermission permission);
}
