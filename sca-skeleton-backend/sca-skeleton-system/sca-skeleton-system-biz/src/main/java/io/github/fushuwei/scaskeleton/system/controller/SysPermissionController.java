package io.github.fushuwei.scaskeleton.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.log.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.github.fushuwei.scaskeleton.system.api.request.DeleteRequest;
import io.github.fushuwei.scaskeleton.system.api.request.permission.PermissionCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.permission.PermissionPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.permission.PermissionUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.permission.PermissionAssignOptionResponse;
import io.github.fushuwei.scaskeleton.system.api.response.permission.PermissionResponse;
import io.github.fushuwei.scaskeleton.system.service.SysPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理 Controller
 *
 * @author Fu Wei
 */
@Tag(name = "权限管理")
@RestController
@RequestMapping("/permission")
@RequiredArgsConstructor
public class SysPermissionController {

    private final SysPermissionService permissionService;

    @Operation(summary = "查询权限列表")
    @GetMapping("/list")
    @RequiresPermission("sys:permission:list")
    public Result<List<PermissionResponse>> list(@RequestParam(required = false) String realm) {
        return Result.ok(permissionService.listAllPermissions(realm));
    }

    @Operation(summary = "查询可授权权限列表", description = "用于租户套餐/角色等功能授权面板")
    @GetMapping("/assign-options")
    @RequiresPermission({
        "sys:role:list",              // 角色管理授权面板
        "sys:tenant-package:list"     // 租户套餐管理授权面板
    })
    public Result<List<PermissionAssignOptionResponse>> assignOptions(@RequestParam(required = false) String realm) {
        return Result.ok(permissionService.listAssignablePermissions(realm));
    }

    @Operation(summary = "查询当前用户菜单列表")
    @GetMapping("/menus")
    public Result<List<PermissionResponse>> menus() {
        return Result.ok(permissionService.listUserMenus());
    }

    @Operation(summary = "分页查询权限列表")
    @GetMapping("/page")
    @RequiresPermission("sys:permission:list")
    public Result<IPage<PermissionResponse>> page(@Validated PermissionPageRequest request) {
        return Result.ok(permissionService.pagePermissions(request));
    }

    @Operation(summary = "查询按钮权限列表")
    @GetMapping("/buttons/{parentId}")
    @RequiresPermission("sys:permission:list")
    public Result<List<PermissionResponse>> buttons(@PathVariable String parentId) {
        return Result.ok(permissionService.listButtonsByParentId(parentId));
    }

    @Operation(summary = "查询权限详情")
    @GetMapping("/{id}")
    @RequiresPermission("sys:permission:list")
    public Result<PermissionResponse> getById(@PathVariable String id) {
        return Result.ok(permissionService.getPermissionById(id));
    }

    @Operation(summary = "创建权限")
    @PostMapping("/create")
    @RequiresPermission("sys:permission:add")
    @OperationLog(module = "菜单管理", action = "添加菜单")
    public Result<Void> create(@Validated @RequestBody PermissionCreateRequest request) {
        permissionService.createPermission(request);
        return Result.ok();
    }

    @Operation(summary = "编辑权限")
    @PostMapping("/update")
    @RequiresPermission("sys:permission:edit")
    @OperationLog(module = "菜单管理", action = "编辑菜单")
    public Result<Void> update(@Validated @RequestBody PermissionUpdateRequest request) {
        permissionService.updatePermission(request);
        return Result.ok();
    }

    @Operation(summary = "删除权限")
    @PostMapping("/delete")
    @RequiresPermission("sys:permission:delete")
    @OperationLog(module = "菜单管理", action = "删除菜单")
    public Result<Void> delete(@Validated @RequestBody DeleteRequest request) {
        permissionService.deletePermission(request.getId());
        return Result.ok();
    }

    @Operation(summary = "批量删除权限")
    @PostMapping("/batch/delete")
    @RequiresPermission("sys:permission:delete")
    @OperationLog(module = "菜单管理", action = "批量删除菜单")
    public Result<Void> batchDelete(@RequestBody List<String> ids) {
        permissionService.batchDeletePermissions(ids);
        return Result.ok();
    }
}
