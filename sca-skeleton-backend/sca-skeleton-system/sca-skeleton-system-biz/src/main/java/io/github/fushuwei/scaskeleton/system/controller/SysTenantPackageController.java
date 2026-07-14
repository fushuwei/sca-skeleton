package io.github.fushuwei.scaskeleton.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.log.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.github.fushuwei.scaskeleton.system.api.request.tenantpackage.TenantPackagePageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenantpackage.TenantPackageCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenantpackage.TenantPackagePermissionAssignRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenantpackage.TenantPackageUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.tenantpackage.TenantPackageResponse;
import io.github.fushuwei.scaskeleton.system.service.SysTenantPackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户套餐管理 Controller。
 *
 * @author Fu Wei
 */
@RestController
@RequestMapping("/tenant-package")
@RequiredArgsConstructor
public class SysTenantPackageController {

    private final SysTenantPackageService packageService;

    // 分页查询套餐列表，需 sys:tenant-package:list
    @GetMapping("/page")
    @RequiresPermission("sys:tenant-package:list")
    public Result<IPage<TenantPackageResponse>> page(@Validated TenantPackagePageRequest request) {
        return Result.ok(packageService.pagePackages(request));
    }

    // 查询套餐列表，需 sys:tenant-package:list
    @GetMapping("/list")
    @RequiresPermission("sys:tenant-package:list")
    public Result<List<TenantPackageResponse>> list() {
        return Result.ok(packageService.listPackages());
    }

    // 按 ID 查询套餐详情，需 sys:tenant-package:query
    @GetMapping("/{id}")
    @RequiresPermission("sys:tenant-package:query")
    public Result<TenantPackageResponse> getById(@PathVariable String id) {
        return Result.ok(packageService.getPackageById(id));
    }

    // 查询套餐已分配的权限 ID 列表，需 sys:tenant-package:query
    @GetMapping("/{id}/permissions")
    @RequiresPermission("sys:tenant-package:query")
    public Result<List<String>> getPermissionIds(@PathVariable String id) {
        return Result.ok(packageService.getPackagePermissionIds(id));
    }

    // 创建套餐，需 sys:tenant-package:add
    @PostMapping("/create")
    @RequiresPermission("sys:tenant-package:add")
    @OperationLog(module = "套餐管理", action = "添加套餐")
    public Result<Void> create(
            @Validated @RequestBody TenantPackageCreateRequest request) {
        packageService.createPackage(request);
        return Result.ok();
    }

    // 更新套餐信息，需 sys:tenant-package:edit
    @PostMapping("/update")
    @RequiresPermission("sys:tenant-package:edit")
    @OperationLog(module = "套餐管理", action = "编辑套餐")
    public Result<Void> update(
            @Validated @RequestBody TenantPackageUpdateRequest request) {
        packageService.updatePackage(request);
        return Result.ok();
    }

    // 删除指定套餐，需 sys:tenant-package:delete
    @PostMapping("/delete")
    @RequiresPermission("sys:tenant-package:delete")
    @OperationLog(module = "套餐管理", action = "删除套餐")
    public Result<Void> delete(@RequestBody String id) {
        packageService.deletePackage(id);
        return Result.ok();
    }

    // 批量删除套餐
    @PostMapping("/batch/delete")
    @RequiresPermission("sys:tenant-package:delete")
    @OperationLog(module = "套餐管理", action = "批量删除套餐")
    public Result<Void> batchDelete(@RequestBody List<String> ids) {
        packageService.batchDeletePackages(ids);
        return Result.ok();
    }

    // 为套餐分配权限，需 sys:tenant-package:assign-permission
    @PostMapping("/assign-permission")
    @RequiresPermission("sys:tenant-package:assign-permission")
    @OperationLog(module = "套餐管理", action = "分配权限")
    public Result<Void> assignPermissions(@Validated @RequestBody TenantPackagePermissionAssignRequest request) {
        packageService.assignPermissions(request.getId(), request.getPermissionIds());
        return Result.ok();
    }
}
