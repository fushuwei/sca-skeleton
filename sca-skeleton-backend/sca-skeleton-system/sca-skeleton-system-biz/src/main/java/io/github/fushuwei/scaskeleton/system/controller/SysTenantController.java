package io.github.fushuwei.scaskeleton.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.logging.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.github.fushuwei.scaskeleton.system.api.request.tenant.TenantCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenant.TenantPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenant.TenantUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.tenant.TenantResponse;
import io.github.fushuwei.scaskeleton.system.service.SysTenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户管理 Controller。
 *
 * @author Fu Wei
 */
@RestController
@RequestMapping("/tenant")
@RequiredArgsConstructor
public class SysTenantController {

    private final SysTenantService tenantService;

    // 分页查询租户列表，需 sys:tenant:list
    @GetMapping("/page")
    @RequiresPermission("sys:tenant:list")
    public Result<IPage<TenantResponse>> page(@Validated TenantPageRequest request) {
        return Result.ok(tenantService.pageTenants(request));
    }

    // 查询租户列表，需 sys:tenant:list
    @GetMapping("/list")
    @RequiresPermission("sys:tenant:list")
    public Result<List<TenantResponse>> list() {
        return Result.ok(tenantService.listTenants());
    }

    // 按 ID 查询租户详情，需 sys:tenant:query
    @GetMapping("/{id}")
    @RequiresPermission("sys:tenant:query")
    public Result<TenantResponse> getById(@PathVariable("id") String id) {
        return Result.ok(tenantService.getTenantById(id));
    }

    // 创建租户，需 sys:tenant:add
    @PostMapping
    @RequiresPermission("sys:tenant:add")
    @OperationLog(module = "租户管理", action = "添加租户", logArgs = false)
    public Result<Void> create(@Validated @RequestBody TenantCreateRequest request) {
        tenantService.createTenant(request);
        return Result.ok();
    }

    // 更新租户信息，需 sys:tenant:edit
    @PutMapping
    @RequiresPermission("sys:tenant:edit")
    @OperationLog(module = "租户管理", action = "编辑租户", logArgs = false)
    public Result<Void> update(@Validated @RequestBody TenantUpdateRequest request) {
        tenantService.updateTenant(request);
        return Result.ok();
    }

    // 删除指定租户，需 sys:tenant:delete
    @DeleteMapping("/{id}")
    @RequiresPermission("sys:tenant:delete")
    @OperationLog(module = "租户管理", action = "删除租户")
    public Result<Void> delete(@PathVariable("id") String id) {
        tenantService.deleteTenant(id);
        return Result.ok();
    }

    // 批量删除租户
    @DeleteMapping("/batch")
    @RequiresPermission("sys:tenant:delete")
    @OperationLog(module = "租户管理", action = "批量删除租户")
    public Result<Void> batchDelete(@RequestBody List<String> ids) {
        tenantService.batchDeleteTenants(ids);
        return Result.ok();
    }
}
