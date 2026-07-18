package io.github.fushuwei.scaskeleton.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.log.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.github.fushuwei.scaskeleton.system.api.request.DeleteRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenantpackage.TenantPackageCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenantpackage.TenantPackagePageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenantpackage.TenantPackagePermissionAssignRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenantpackage.TenantPackageUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.tenantpackage.TenantPackageOptionResponse;
import io.github.fushuwei.scaskeleton.system.api.response.tenantpackage.TenantPackageResponse;
import io.github.fushuwei.scaskeleton.system.service.SysTenantPackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户套餐管理 Controller
 *
 * @author Fu Wei
 */
@Tag(name = "租户套餐管理")
@RestController
@RequestMapping("/tenant-package")
@RequiredArgsConstructor
public class SysTenantPackageController {

    private final SysTenantPackageService packageService;

    @Operation(summary = "查询套餐列表")
    @GetMapping("/list")
    @RequiresPermission("sys:tenant-package:list")
    public Result<List<TenantPackageResponse>> list() {
        return Result.ok(packageService.listPackages());
    }

    @Operation(summary = "查询套餐选项列表", description = "用于租户管理等功能表单下拉选择")
    @GetMapping("/options")
    @RequiresPermission("sys:tenant:list")
    public Result<List<TenantPackageOptionResponse>> options() {
        return Result.ok(packageService.listPackageOptions());
    }

    @Operation(summary = "分页查询套餐列表")
    @GetMapping("/page")
    @RequiresPermission("sys:tenant-package:list")
    public Result<IPage<TenantPackageResponse>> page(@Validated TenantPackagePageRequest request) {
        return Result.ok(packageService.pagePackages(request));
    }

    @Operation(summary = "查询套餐详情")
    @GetMapping("/{id}")
    @RequiresPermission("sys:tenant-package:list")
    public Result<TenantPackageResponse> getById(@PathVariable String id) {
        return Result.ok(packageService.getPackageById(id));
    }

    @Operation(summary = "查询套餐已分配权限")
    @GetMapping("/{id}/permissions")
    @RequiresPermission("sys:tenant-package:list")
    public Result<List<String>> getPermissionIds(@PathVariable String id) {
        return Result.ok(packageService.getPackagePermissionIds(id));
    }

    @Operation(summary = "创建套餐")
    @PostMapping("/create")
    @RequiresPermission("sys:tenant-package:add")
    @OperationLog(module = "套餐管理", action = "添加套餐")
    public Result<Void> create(@Validated @RequestBody TenantPackageCreateRequest request) {
        packageService.createPackage(request);
        return Result.ok();
    }

    @Operation(summary = "编辑套餐")
    @PostMapping("/update")
    @RequiresPermission("sys:tenant-package:edit")
    @OperationLog(module = "套餐管理", action = "编辑套餐")
    public Result<Void> update(@Validated @RequestBody TenantPackageUpdateRequest request) {
        packageService.updatePackage(request);
        return Result.ok();
    }

    @Operation(summary = "删除套餐")
    @PostMapping("/delete")
    @RequiresPermission("sys:tenant-package:delete")
    @OperationLog(module = "套餐管理", action = "删除套餐")
    public Result<Void> delete(@Validated @RequestBody DeleteRequest request) {
        packageService.deletePackage(request.getId());
        return Result.ok();
    }

    @Operation(summary = "批量删除套餐")
    @PostMapping("/batch/delete")
    @RequiresPermission("sys:tenant-package:delete")
    @OperationLog(module = "套餐管理", action = "批量删除套餐")
    public Result<Void> batchDelete(@RequestBody List<String> ids) {
        packageService.batchDeletePackages(ids);
        return Result.ok();
    }

    @Operation(summary = "为套餐分配权限")
    @PostMapping("/assign-permission")
    @RequiresPermission("sys:tenant-package:assign-permission")
    @OperationLog(module = "套餐管理", action = "分配权限")
    public Result<Void> assignPermissions(@Validated @RequestBody TenantPackagePermissionAssignRequest request) {
        packageService.assignPermissions(request);
        return Result.ok();
    }
}
