package io.github.fushuwei.scaskeleton.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.log.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.github.fushuwei.scaskeleton.system.api.request.tenant.TenantCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenant.TenantPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenant.TenantUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.tenant.TenantResponse;
import io.github.fushuwei.scaskeleton.system.service.SysTenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户管理 Controller
 *
 * @author Fu Wei
 */
@Tag(name = "租户管理")
@RestController
@RequestMapping("/tenant")
@RequiredArgsConstructor
public class SysTenantController {

    private final SysTenantService tenantService;

    @Operation(summary = "查询租户列表")
    @GetMapping("/list")
    @RequiresPermission("sys:tenant:list")
    public Result<List<TenantResponse>> list() {
        return Result.ok(tenantService.listTenants());
    }

    @Operation(summary = "分页查询租户列表")
    @GetMapping("/page")
    @RequiresPermission("sys:tenant:list")
    public Result<IPage<TenantResponse>> page(@Validated TenantPageRequest request) {
        return Result.ok(tenantService.pageTenants(request));
    }

    @Operation(summary = "查询租户详情")
    @GetMapping("/{id}")
    @RequiresPermission("sys:tenant:query")
    public Result<TenantResponse> getById(@PathVariable String id) {
        return Result.ok(tenantService.getTenantById(id));
    }

    @Operation(summary = "创建租户")
    @PostMapping("/create")
    @RequiresPermission("sys:tenant:add")
    @OperationLog(module = "租户管理", action = "添加租户")
    public Result<Void> create(@Validated @RequestBody TenantCreateRequest request) {
        tenantService.createTenant(request);
        return Result.ok();
    }

    @Operation(summary = "编辑租户")
    @PostMapping("/update")
    @RequiresPermission("sys:tenant:edit")
    @OperationLog(module = "租户管理", action = "编辑租户")
    public Result<Void> update(@Validated @RequestBody TenantUpdateRequest request) {
        tenantService.updateTenant(request);
        return Result.ok();
    }

    @Operation(summary = "删除租户")
    @PostMapping("/delete")
    @RequiresPermission("sys:tenant:delete")
    @OperationLog(module = "租户管理", action = "删除租户")
    public Result<Void> delete(@RequestBody String id) {
        tenantService.deleteTenant(id);
        return Result.ok();
    }

    @Operation(summary = "批量删除租户")
    @PostMapping("/batch/delete")
    @RequiresPermission("sys:tenant:delete")
    @OperationLog(module = "租户管理", action = "批量删除租户")
    public Result<Void> batchDelete(@RequestBody List<String> ids) {
        tenantService.batchDeleteTenants(ids);
        return Result.ok();
    }
}
