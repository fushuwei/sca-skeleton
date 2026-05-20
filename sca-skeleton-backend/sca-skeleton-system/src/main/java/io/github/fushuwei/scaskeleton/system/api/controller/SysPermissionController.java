package io.github.fushuwei.scaskeleton.system.api.controller;

import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.system.api.dto.permission.PermissionSaveRequest;
import io.github.fushuwei.scaskeleton.system.application.service.SysPermissionService;
import io.github.fushuwei.scaskeleton.system.infrastructure.entity.SysPermission;
import lombok.RequiredArgsConstructor;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理 Controller。
 *
 * @author Fu Wei
 */
@RestController
@RequestMapping("/permission")
@RequiredArgsConstructor
public class SysPermissionController {

    private final SysPermissionService permissionService;

    @GetMapping("/list")
    @RequiresPermission("sys:permission:list")
    public Result<List<SysPermission>> list() {
        return Result.ok(permissionService.listAllPermissions());
    }

    @GetMapping("/{id}")
    @RequiresPermission("sys:permission:query")
    public Result<SysPermission> getById(@PathVariable String id) {
        return Result.ok(permissionService.getPermissionById(id));
    }

    @PostMapping
    @RequiresPermission("sys:permission:add")
    public Result<Void> create(@Validated @RequestBody PermissionSaveRequest request) {
        permissionService.createPermission(request);
        return Result.ok();
    }

    @PutMapping
    @RequiresPermission("sys:permission:edit")
    public Result<Void> update(@Validated @RequestBody PermissionSaveRequest request) {
        permissionService.updatePermission(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("sys:permission:delete")
    public Result<Void> delete(@PathVariable String id) {
        permissionService.deletePermission(id);
        return Result.ok();
    }
}
