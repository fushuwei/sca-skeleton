package io.github.fushuwei.scaskeleton.system.application.service;

import io.github.fushuwei.scaskeleton.system.api.dto.permission.PermissionSaveRequest;
import io.github.fushuwei.scaskeleton.system.infrastructure.entity.SysPermission;

import java.util.List;

/**
 * 权限管理服务接口。
 *
 * @author Fu Wei
 */
public interface SysPermissionService {

    /** 查询全量权限树（用于权限分配界面） */
    List<SysPermission> listAllPermissions();

    SysPermission getPermissionById(String id);

    void createPermission(PermissionSaveRequest request);

    void updatePermission(PermissionSaveRequest request);

    void deletePermission(String id);
}
