package io.github.fushuwei.scaskeleton.system.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.core.validation.ValidGroup;
import io.github.fushuwei.scaskeleton.logging.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import io.github.fushuwei.scaskeleton.system.api.request.role.RolePageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.role.RoleSaveRequest;
import io.github.fushuwei.scaskeleton.system.api.response.role.RoleResponse;
import io.github.fushuwei.scaskeleton.system.service.SysRoleService;
import lombok.RequiredArgsConstructor;
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

    // 分页查询当前租户下角色列表，需 sys:role:list
    @GetMapping("/page")
    @RequiresPermission("sys:role:list")
    public Result<IPage<RoleResponse>> page(@Validated RolePageRequest request) {
        return Result.ok(roleService.pageRoles(SecurityUtils.getTenantId(), request));
    }

    // 查询当前租户下角色列表，需 sys:role:list
    @GetMapping("/list")
    @RequiresPermission("sys:role:list")
    public Result<List<RoleResponse>> list() {
        return Result.ok(roleService.listRoles(SecurityUtils.getTenantId()));
    }

    // 按 ID 查询角色详情，需 sys:role:query
    @GetMapping("/{id}")
    @RequiresPermission("sys:role:query")
    public Result<RoleResponse> getById(@PathVariable("id") String id) {
        return Result.ok(roleService.getRoleById(id));
    }

    // 查询角色已分配的权限 ID 列表，需 sys:role:query
    @GetMapping("/{id}/permissions")
    @RequiresPermission("sys:role:query")
    public Result<List<String>> getPermissionIds(@PathVariable("id") String id) {
        return Result.ok(roleService.getRolePermissionIds(id));
    }

    // 在当前租户下创建角色，需 sys:role:add
    @PostMapping
    @RequiresPermission("sys:role:add")
    @OperationLog(module = "角色管理", action = "添加角色", logArgs = false)
    public Result<Void> create(
            @Validated(ValidGroup.Create.class) @RequestBody RoleSaveRequest request) {
        roleService.createRole(SecurityUtils.getTenantId(), request);
        return Result.ok();
    }

    // 更新当前租户下角色信息，需 sys:role:edit
    @PutMapping
    @RequiresPermission("sys:role:edit")
    @OperationLog(module = "角色管理", action = "编辑角色", logArgs = false)
    public Result<Void> update(
            @Validated(ValidGroup.Update.class) @RequestBody RoleSaveRequest request) {
        roleService.updateRole(SecurityUtils.getTenantId(), request);
        return Result.ok();
    }

    // 删除指定角色，需 sys:role:delete
    @DeleteMapping("/{id}")
    @RequiresPermission("sys:role:delete")
    @OperationLog(module = "角色管理", action = "删除角色")
    public Result<Void> delete(@PathVariable("id") String id) {
        roleService.deleteRole(id);
        return Result.ok();
    }

    // 为角色分配权限，需 sys:role:assign-permission；按当前租户隔离
    @PutMapping("/{id}/permissions")
    @RequiresPermission("sys:role:assign-permission")
    @OperationLog(module = "角色管理", action = "分配权限", logArgs = false)
    public Result<Void> assignPermissions(@PathVariable("id") String id,
                                              @RequestBody List<String> permissionIds) {
        roleService.assignPermissions(SecurityUtils.getTenantId(), id, permissionIds);
        return Result.ok();
    }
}
