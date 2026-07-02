package io.github.fushuwei.scaskeleton.system.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.logging.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.github.fushuwei.scaskeleton.system.api.dto.permission.PermissionPageRequest;
import io.github.fushuwei.scaskeleton.system.api.dto.permission.PermissionSaveRequest;
import io.github.fushuwei.scaskeleton.system.application.service.SysPermissionService;
import io.github.fushuwei.scaskeleton.system.infrastructure.entity.SysPermission;
import lombok.RequiredArgsConstructor;
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

    // 查询全部权限树/列表，需 sys:permission:list（平台级权限定义，无租户隔离）
    @GetMapping("/list")
    @RequiresPermission("sys:permission:list")
    public Result<List<SysPermission>> list() {
        return Result.ok(permissionService.listAllPermissions());
    }

    // 分页查询指定父节点下的子权限列表，需 sys:permission:list
    @GetMapping("/page")
    @RequiresPermission("sys:permission:list")
    public Result<IPage<SysPermission>> page(@Validated PermissionPageRequest request) {
        return Result.ok(permissionService.pagePermissions(request));
    }

    // 查询指定父节点下的按钮权限列表（用于列表行展开），需 sys:permission:list
    @GetMapping("/buttons/{parentId}")
    @RequiresPermission("sys:permission:list")
    public Result<List<SysPermission>> buttons(@PathVariable String parentId) {
        return Result.ok(permissionService.listButtonsByParentId(parentId));
    }

    // 按 ID 查询权限详情，需 sys:permission:query
    @GetMapping("/{id}")
    @RequiresPermission("sys:permission:query")
    public Result<SysPermission> getById(@PathVariable String id) {
        return Result.ok(permissionService.getPermissionById(id));
    }

    // 新增权限节点，需 sys:permission:add
    @PostMapping
    @RequiresPermission("sys:permission:add")
    @OperationLog(module = "菜单管理", action = "添加菜单")
    public Result<Void> create(@Validated @RequestBody PermissionSaveRequest request) {
        permissionService.createPermission(request);
        return Result.ok();
    }

    // 更新权限节点，需 sys:permission:edit
    @PutMapping
    @RequiresPermission("sys:permission:edit")
    @OperationLog(module = "菜单管理", action = "编辑菜单")
    public Result<Void> update(@Validated @RequestBody PermissionSaveRequest request) {
        permissionService.updatePermission(request);
        return Result.ok();
    }

    // 删除权限节点，需 sys:permission:delete
    @DeleteMapping("/{id}")
    @RequiresPermission("sys:permission:delete")
    @OperationLog(module = "菜单管理", action = "删除菜单")
    public Result<Void> delete(@PathVariable String id) {
        permissionService.deletePermission(id);
        return Result.ok();
    }
}
