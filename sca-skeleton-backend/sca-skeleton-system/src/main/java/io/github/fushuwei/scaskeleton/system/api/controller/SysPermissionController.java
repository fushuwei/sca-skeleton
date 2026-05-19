package io.github.fushuwei.scaskeleton.system.api.controller;

import io.github.fushuwei.scaskeleton.web.response.ApiResponse;
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
    public ApiResponse<List<SysPermission>> list() {
        return ApiResponse.success(permissionService.listAllPermissions());
    }

    @GetMapping("/{id}")
    @RequiresPermission("sys:permission:query")
    public ApiResponse<SysPermission> getById(@PathVariable String id) {
        return ApiResponse.success(permissionService.getPermissionById(id));
    }

    @PostMapping
    @RequiresPermission("sys:permission:add")
    public ApiResponse<Void> create(@Validated @RequestBody PermissionSaveRequest request) {
        permissionService.createPermission(request);
        return ApiResponse.success();
    }

    @PutMapping
    @RequiresPermission("sys:permission:edit")
    public ApiResponse<Void> update(@Validated @RequestBody PermissionSaveRequest request) {
        permissionService.updatePermission(request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("sys:permission:delete")
    public ApiResponse<Void> delete(@PathVariable String id) {
        permissionService.deletePermission(id);
        return ApiResponse.success();
    }
}
