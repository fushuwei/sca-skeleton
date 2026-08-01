package io.github.fushuwei.scaskeleton.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.log.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.github.fushuwei.scaskeleton.system.api.request.DeleteRequest;
import io.github.fushuwei.scaskeleton.system.api.request.role.RoleCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.role.RolePageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.role.RoleUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.permission.PermissionAssignOptionResponse;
import io.github.fushuwei.scaskeleton.system.api.response.role.RoleOptionResponse;
import io.github.fushuwei.scaskeleton.system.api.response.role.RoleResponse;
import io.github.fushuwei.scaskeleton.system.service.SysPermissionService;
import io.github.fushuwei.scaskeleton.system.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理 Controller
 *
 * @author Fu Wei
 */
@Tag(name = "角色管理")
@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    private final SysPermissionService permissionService;

    @Operation(summary = "查询角色列表")
    @GetMapping("/list")
    @RequiresPermission("sys:role:list")
    public Result<List<RoleResponse>> list() {
        return Result.ok(roleService.listRoles());
    }

    @Operation(summary = "查询角色授权面板可分配权限", description = "超管按指定租户套餐过滤，非超管按自身权限过滤")
    @GetMapping("/assign-options")
    @RequiresPermission("sys:role:list")
    public Result<List<PermissionAssignOptionResponse>> assignOptions(
        @RequestParam(required = false) String realm,
        @RequestParam(required = false) String tenantId) {
        return Result.ok(permissionService.listPermissionsForRole(realm, tenantId));
    }

    @Operation(summary = "查询角色选项列表", description = "用于用户管理等功能表单下拉选择")
    @GetMapping("/options")
    @RequiresPermission("sys:user:list")
    public Result<List<RoleOptionResponse>> options(@RequestParam(required = false) String tenantId, @RequestParam(required = false) String realm) {
        return Result.ok(roleService.listRoleOptions(tenantId, realm));
    }

    @Operation(summary = "分页查询角色列表")
    @GetMapping("/page")
    @RequiresPermission("sys:role:list")
    public Result<IPage<RoleResponse>> page(@Validated RolePageRequest request) {
        return Result.ok(roleService.pageRoles(request));
    }

    @Operation(summary = "根据 ID 查询角色详情")
    @GetMapping("/{id}")
    @RequiresPermission("sys:role:list")
    public Result<RoleResponse> getById(@PathVariable String id) {
        return Result.ok(roleService.getRoleById(id));
    }

    @Operation(summary = "查询角色已分配权限")
    @GetMapping("/{id}/permissions")
    @RequiresPermission("sys:role:list")
    public Result<List<String>> getPermissionIds(@PathVariable String id) {
        return Result.ok(roleService.getRolePermissionIds(id));
    }

    @Operation(summary = "新增角色")
    @PostMapping("/create")
    @RequiresPermission("sys:role:add")
    @OperationLog(module = "角色管理", action = "新增角色")
    public Result<Void> create(@Validated @RequestBody RoleCreateRequest request) {
        roleService.createRole(request);
        return Result.ok();
    }

    @Operation(summary = "编辑角色")
    @PostMapping("/update")
    @RequiresPermission("sys:role:edit")
    @OperationLog(module = "角色管理", action = "编辑角色")
    public Result<Void> update(@Validated @RequestBody RoleUpdateRequest request) {
        roleService.updateRole(request);
        return Result.ok();
    }

    @Operation(summary = "删除角色")
    @PostMapping("/delete")
    @RequiresPermission("sys:role:delete")
    @OperationLog(module = "角色管理", action = "删除角色")
    public Result<Void> delete(@Validated @RequestBody DeleteRequest request) {
        roleService.deleteRole(request.getId());
        return Result.ok();
    }

    @Operation(summary = "批量删除角色")
    @PostMapping("/batch/delete")
    @RequiresPermission("sys:role:delete")
    @OperationLog(module = "角色管理", action = "批量删除角色")
    public Result<Void> batchDelete(@RequestBody List<String> ids) {
        roleService.batchDeleteRoles(ids);
        return Result.ok();
    }
}
