package io.github.fushuwei.scaskeleton.system.application.service;

import io.github.fushuwei.scaskeleton.system.api.dto.role.RoleSaveRequest;
import io.github.fushuwei.scaskeleton.system.infrastructure.entity.SysRole;

import java.util.List;

/**
 * 角色管理服务接口。
 *
 * @author Fu Wei
 */
public interface SysRoleService {

    List<SysRole> listRoles(String tenantId);

    SysRole getRoleById(String id);

    void createRole(String tenantId, RoleSaveRequest request);

    void updateRole(String tenantId, RoleSaveRequest request);

    void deleteRole(String id);

    /** 为角色分配权限（全量替换） */
    void assignPermissions(String tenantId, String roleId, List<String> permissionIds);
}
