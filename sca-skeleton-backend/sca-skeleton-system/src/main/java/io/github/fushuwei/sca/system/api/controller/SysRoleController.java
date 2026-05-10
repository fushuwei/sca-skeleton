package io.github.fushuwei.sca.system.api.controller;

import io.github.fushuwei.sca.starter.security.context.SecurityUtils;
import io.github.fushuwei.sca.starter.web.response.ApiResponse;
import io.github.fushuwei.sca.system.api.dto.role.RoleSaveRequest;
import io.github.fushuwei.sca.system.application.service.SysRoleService;
import io.github.fushuwei.sca.system.infrastructure.entity.SysRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAuthority('sys:role:list')")
    public ApiResponse<List<SysRole>> list() {
        return ApiResult.success(roleService.listRoles(SecurityUtils.getTenantId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:role:query')")
    public ApiResponse<SysRole> getById(@PathVariable String id) {
        return ApiResult.success(roleService.getRoleById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sys:role:add')")
    public ApiResponse<Void> create(@Validated @RequestBody RoleSaveRequest request) {
        roleService.createRole(SecurityUtils.getTenantId(), request);
        return ApiResult.success();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('sys:role:edit')")
    public ApiResponse<Void> update(@Validated @RequestBody RoleSaveRequest request) {
        roleService.updateRole(SecurityUtils.getTenantId(), request);
        return ApiResult.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:role:delete')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        roleService.deleteRole(id);
        return ApiResult.success();
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('sys:role:assign-permission')")
    public ApiResponse<Void> assignPermissions(@PathVariable String id,
                                              @RequestBody List<String> permissionIds) {
        roleService.assignPermissions(SecurityUtils.getTenantId(), id, permissionIds);
        return ApiResult.success();
    }
}
