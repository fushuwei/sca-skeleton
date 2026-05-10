package io.github.fushuwei.sca.system.api.controller;

import io.github.fushuwei.sca.starter.web.response.ApiResponse;
import io.github.fushuwei.sca.system.api.dto.permission.PermissionSaveRequest;
import io.github.fushuwei.sca.system.application.service.SysPermissionService;
import io.github.fushuwei.sca.system.infrastructure.entity.SysPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAuthority('sys:permission:list')")
    public ApiResponse<List<SysPermission>> list() {
        return ApiResult.success(permissionService.listAllPermissions());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:permission:query')")
    public ApiResponse<SysPermission> getById(@PathVariable String id) {
        return ApiResult.success(permissionService.getPermissionById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sys:permission:add')")
    public ApiResponse<Void> create(@Validated @RequestBody PermissionSaveRequest request) {
        permissionService.createPermission(request);
        return ApiResult.success();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('sys:permission:edit')")
    public ApiResponse<Void> update(@Validated @RequestBody PermissionSaveRequest request) {
        permissionService.updatePermission(request);
        return ApiResult.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:permission:delete')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        permissionService.deletePermission(id);
        return ApiResult.success();
    }
}
