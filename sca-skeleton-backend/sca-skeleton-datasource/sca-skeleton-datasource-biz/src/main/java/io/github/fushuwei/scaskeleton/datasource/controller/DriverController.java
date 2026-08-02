package io.github.fushuwei.scaskeleton.datasource.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.datasource.api.enums.DbType;
import io.github.fushuwei.scaskeleton.datasource.api.request.DeleteRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.driver.DriverCreateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.driver.DriverPageRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.driver.DriverUpdateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.response.driver.DriverOptionResponse;
import io.github.fushuwei.scaskeleton.datasource.api.response.driver.DriverResponse;
import io.github.fushuwei.scaskeleton.datasource.api.response.driver.DriverUploadResponse;
import io.github.fushuwei.scaskeleton.datasource.service.DriverService;
import io.github.fushuwei.scaskeleton.log.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 驱动管理 Controller
 * <p>
 * 权限码：sys:datasource:driver:list/add/edit/delete
 *
 * @author Fu Wei
 */
@Tag(name = "驱动管理")
@RestController
@RequestMapping("/driver")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @Operation(summary = "分页查询驱动列表")
    @GetMapping("/page")
    @RequiresPermission("sys:datasource:driver:list")
    public Result<IPage<DriverResponse>> page(@Validated DriverPageRequest request) {
        return Result.ok(driverService.pageDrivers(request));
    }

    @Operation(summary = "根据 ID 查询驱动详情")
    @GetMapping("/{id}")
    @RequiresPermission("sys:datasource:driver:list")
    public Result<DriverResponse> getById(@PathVariable String id) {
        return Result.ok(driverService.getDriverById(id));
    }

    @Operation(summary = "新增驱动")
    @PostMapping("/create")
    @RequiresPermission("sys:datasource:driver:add")
    @OperationLog(module = "驱动管理", action = "新增驱动")
    public Result<Void> create(@Validated @RequestBody DriverCreateRequest request) {
        driverService.createDriver(request);
        return Result.ok();
    }

    @Operation(summary = "上传驱动 JAR 文件")
    @PostMapping("/upload")
    @RequiresPermission("sys:datasource:driver:add")
    @OperationLog(module = "驱动管理", action = "上传驱动JAR")
    public Result<DriverUploadResponse> upload(@RequestParam DbType dbType,
                                               @RequestParam("file") MultipartFile file) {
        return Result.ok(driverService.uploadDriver(dbType, file));
    }

    @Operation(summary = "编辑驱动")
    @PostMapping("/update")
    @RequiresPermission("sys:datasource:driver:edit")
    @OperationLog(module = "驱动管理", action = "编辑驱动")
    public Result<Void> update(@Validated @RequestBody DriverUpdateRequest request) {
        driverService.updateDriver(request);
        return Result.ok();
    }

    @Operation(summary = "删除驱动")
    @PostMapping("/delete")
    @RequiresPermission("sys:datasource:driver:delete")
    @OperationLog(module = "驱动管理", action = "删除驱动")
    public Result<Void> delete(@Validated @RequestBody DeleteRequest request) {
        driverService.deleteDriver(request.getId());
        return Result.ok();
    }

    @Operation(summary = "批量删除驱动")
    @PostMapping("/batch/delete")
    @RequiresPermission("sys:datasource:driver:delete")
    @OperationLog(module = "驱动管理", action = "批量删除驱动")
    public Result<Void> batchDelete(@RequestBody List<String> ids) {
        driverService.batchDeleteDrivers(ids);
        return Result.ok();
    }

    @Operation(summary = "启用驱动")
    @PostMapping("/{id}/enable")
    @RequiresPermission("sys:datasource:driver:edit")
    @OperationLog(module = "驱动管理", action = "启用驱动")
    public Result<Void> enable(@PathVariable String id) {
        driverService.changeStatus(id, "enabled");
        return Result.ok();
    }

    @Operation(summary = "禁用驱动")
    @PostMapping("/{id}/disable")
    @RequiresPermission("sys:datasource:driver:edit")
    @OperationLog(module = "驱动管理", action = "禁用驱动")
    public Result<Void> disable(@PathVariable String id) {
        driverService.changeStatus(id, "disabled");
        return Result.ok();
    }

    @Operation(summary = "查询驱动选项列表")
    @GetMapping("/options")
    @RequiresPermission("sys:datasource:driver:list")
    public Result<List<DriverOptionResponse>> options(@RequestParam(required = false) DbType dbType) {
        return Result.ok(driverService.listDriverOptions(dbType));
    }
}
