package io.github.fushuwei.sca.system.api.controller;

import io.github.fushuwei.sca.starter.security.context.SecurityUtils;
import io.github.fushuwei.sca.starter.web.response.ApiResponse;
import io.github.fushuwei.sca.system.api.dto.role.RoleSaveRequest;
import io.github.fushuwei.sca.system.application.service.SysRoleService;
import io.github.fushuwei.sca.system.infrastructure.entity.SysRole;
import lombok.RequiredArgsConstructor;
import io.github.fushuwei.sca.starter.security.annotation.RequiresPermission;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理 Controller。
 *
 * @author Fu Wei
 */
@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @GetMapping("/list")
    @RequiresPermission("sys:role:list")
    public ApiResponse<List<SysRole>> list() {
        return ApiResponse.success(roleService.listRoles(SecurityUtils.getTenantId()));
    }

    @GetMapping("/{id}")
    @RequiresPermission("sys:role:query")
    public ApiResponse<SysRole> getById(@PathVariable String id) {
        return ApiResponse.success(roleService.getRoleById(id));
    }

    @PostMapping
    @RequiresPermission("sys:role:add")
    public ApiResponse<Void> create(@Validated @RequestBody RoleSaveRequest request) {
        roleService.createRole(SecurityUtils.getTenantId(), request);
        return ApiResponse.success();
    }

    @PutMapping
    @RequiresPermission("sys:role:edit")
    public ApiResponse<Void> update(@Validated @RequestBody RoleSaveRequest request) {
        roleService.updateRole(SecurityUtils.getTenantId(), request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("sys:role:delete")
    public ApiResponse<Void> delete(@PathVariable String id) {
        roleService.deleteRole(id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/permissions")
    @RequiresPermission("sys:role:assign-permission")
    public ApiResponse<Void> assignPermissions(@PathVariable String id,
                                              @RequestBody List<String> permissionIds) {
        roleService.assignPermissions(SecurityUtils.getTenantId(), id, permissionIds);
        return ApiResponse.success();
    }
}
