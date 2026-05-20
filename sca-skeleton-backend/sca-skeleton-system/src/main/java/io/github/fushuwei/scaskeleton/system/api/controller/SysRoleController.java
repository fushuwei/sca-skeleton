package io.github.fushuwei.scaskeleton.system.api.controller;

import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import io.github.fushuwei.scaskeleton.system.api.dto.role.RoleSaveRequest;
import io.github.fushuwei.scaskeleton.system.application.service.SysRoleService;
import io.github.fushuwei.scaskeleton.system.infrastructure.entity.SysRole;
import lombok.RequiredArgsConstructor;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
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
    public Result<List<SysRole>> list() {
        return Result.ok(roleService.listRoles(SecurityUtils.getTenantId()));
    }

    @GetMapping("/{id}")
    @RequiresPermission("sys:role:query")
    public Result<SysRole> getById(@PathVariable String id) {
        return Result.ok(roleService.getRoleById(id));
    }

    @PostMapping
    @RequiresPermission("sys:role:add")
    public Result<Void> create(@Validated @RequestBody RoleSaveRequest request) {
        roleService.createRole(SecurityUtils.getTenantId(), request);
        return Result.ok();
    }

    @PutMapping
    @RequiresPermission("sys:role:edit")
    public Result<Void> update(@Validated @RequestBody RoleSaveRequest request) {
        roleService.updateRole(SecurityUtils.getTenantId(), request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("sys:role:delete")
    public Result<Void> delete(@PathVariable String id) {
        roleService.deleteRole(id);
        return Result.ok();
    }

    @PutMapping("/{id}/permissions")
    @RequiresPermission("sys:role:assign-permission")
    public Result<Void> assignPermissions(@PathVariable String id,
                                              @RequestBody List<String> permissionIds) {
        roleService.assignPermissions(SecurityUtils.getTenantId(), id, permissionIds);
        return Result.ok();
    }
}
