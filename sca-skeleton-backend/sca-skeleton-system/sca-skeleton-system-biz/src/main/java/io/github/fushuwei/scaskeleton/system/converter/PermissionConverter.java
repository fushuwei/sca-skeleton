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

    /**
     * 权限实体 → 权限响应
     *
     * @param permission 权限实体
     * @return 权限响应对象
     */
    PermissionResponse toPermissionResponse(SysPermission permission);
}
