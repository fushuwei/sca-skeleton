package io.github.fushuwei.scaskeleton.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.log.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.github.fushuwei.scaskeleton.system.api.request.DeleteRequest;
import io.github.fushuwei.scaskeleton.system.api.request.config.ConfigCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.config.ConfigPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.config.ConfigStatusRequest;
import io.github.fushuwei.scaskeleton.system.api.request.config.ConfigUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.config.ConfigResponse;
import io.github.fushuwei.scaskeleton.system.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统配置管理 Controller
 *
 * @author Fu Wei
 */
@Tag(name = "系统配置管理")
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService configService;

    @Operation(summary = "查询系统配置列表")
    @GetMapping("/list")
    @RequiresPermission("sys:config:list")
    public Result<List<ConfigResponse>> list() {
        return Result.ok(configService.listConfigs());
    }

    @Operation(summary = "分页查询系统配置列表")
    @GetMapping("/page")
    @RequiresPermission("sys:config:list")
    public Result<IPage<ConfigResponse>> page(@Validated ConfigPageRequest request) {
        return Result.ok(configService.pageConfigs(request));
    }

    @Operation(summary = "根据 ID 查询系统配置详情")
    @GetMapping("/{id}")
    @RequiresPermission("sys:config:list")
    public Result<ConfigResponse> getById(@PathVariable String id) {
        return Result.ok(configService.getConfigById(id));
    }

    @Operation(summary = "新增系统配置")
    @PostMapping("/create")
    @RequiresPermission("sys:config:add")
    @OperationLog(module = "系统配置管理", action = "新增系统配置")
    public Result<Void> create(@Validated @RequestBody ConfigCreateRequest request) {
        configService.createConfig(request);
        return Result.ok();
    }

    @Operation(summary = "编辑系统配置")
    @PostMapping("/update")
    @RequiresPermission("sys:config:edit")
    @OperationLog(module = "系统配置管理", action = "编辑系统配置")
    public Result<Void> update(@Validated @RequestBody ConfigUpdateRequest request) {
        configService.updateConfig(request);
        return Result.ok();
    }

    @Operation(summary = "启用/禁用系统配置")
    @PostMapping("/status")
    @RequiresPermission("sys:config:edit")
    @OperationLog(module = "系统配置管理", action = "启用/禁用系统配置")
    public Result<Void> updateStatus(@Validated @RequestBody ConfigStatusRequest request) {
        configService.updateConfigStatus(request);
        return Result.ok();
    }

    @Operation(summary = "删除系统配置")
    @PostMapping("/delete")
    @RequiresPermission("sys:config:delete")
    @OperationLog(module = "系统配置管理", action = "删除系统配置")
    public Result<Void> delete(@Validated @RequestBody DeleteRequest request) {
        configService.deleteConfig(request.getId());
        return Result.ok();
    }

    @Operation(summary = "批量删除系统配置")
    @PostMapping("/batch/delete")
    @RequiresPermission("sys:config:delete")
    @OperationLog(module = "系统配置管理", action = "批量删除系统配置")
    public Result<Void> batchDelete(@RequestBody List<String> ids) {
        configService.batchDeleteConfigs(ids);
        return Result.ok();
    }
}
