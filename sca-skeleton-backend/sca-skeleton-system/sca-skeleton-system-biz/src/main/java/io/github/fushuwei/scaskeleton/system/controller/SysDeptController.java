package io.github.fushuwei.scaskeleton.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.log.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.github.fushuwei.scaskeleton.system.api.request.DeleteRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dept.DeptCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dept.DeptPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dept.DeptUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.dept.DeptOptionResponse;
import io.github.fushuwei.scaskeleton.system.api.response.dept.DeptResponse;
import io.github.fushuwei.scaskeleton.system.service.SysDeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理 Controller
 *
 * @author Fu Wei
 */
@Tag(name = "部门管理")
@RestController
@RequestMapping("/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService deptService;

    @Operation(summary = "查询部门列表")
    @GetMapping("/list")
    @RequiresPermission("sys:dept:list")
    public Result<List<DeptResponse>> list() {
        return Result.ok(deptService.listDepts());
    }

    @Operation(summary = "查询部门选项列表", description = "用于用户管理等功能表单下拉选择")
    @GetMapping("/options")
    @RequiresPermission("sys:user:list")
    public Result<List<DeptOptionResponse>> options() {
        return Result.ok(deptService.listDeptOptions());
    }

    @Operation(summary = "分页查询部门列表")
    @GetMapping("/page")
    @RequiresPermission("sys:dept:list")
    public Result<IPage<DeptResponse>> page(@Validated DeptPageRequest request) {
        return Result.ok(deptService.pageDepts(request));
    }

    @Operation(summary = "根据 ID 查询部门详情")
    @GetMapping("/{id}")
    @RequiresPermission("sys:dept:list")
    public Result<DeptResponse> getById(@PathVariable String id) {
        return Result.ok(deptService.getDeptById(id));
    }

    @Operation(summary = "新增部门")
    @PostMapping("/create")
    @RequiresPermission("sys:dept:add")
    @OperationLog(module = "部门管理", action = "新增部门")
    public Result<Void> create(@Validated @RequestBody DeptCreateRequest request) {
        deptService.createDept(request);
        return Result.ok();
    }

    @Operation(summary = "编辑部门")
    @PostMapping("/update")
    @RequiresPermission("sys:dept:edit")
    @OperationLog(module = "部门管理", action = "编辑部门")
    public Result<Void> update(@Validated @RequestBody DeptUpdateRequest request) {
        deptService.updateDept(request);
        return Result.ok();
    }

    @Operation(summary = "删除部门")
    @PostMapping("/delete")
    @RequiresPermission("sys:dept:delete")
    @OperationLog(module = "部门管理", action = "删除部门")
    public Result<Void> delete(@Validated @RequestBody DeleteRequest request) {
        deptService.deleteDept(request.getId());
        return Result.ok();
    }

    @Operation(summary = "批量删除部门")
    @PostMapping("/batch/delete")
    @RequiresPermission("sys:dept:delete")
    @OperationLog(module = "部门管理", action = "批量删除部门")
    public Result<Void> batchDelete(@RequestBody List<String> ids) {
        deptService.batchDeleteDepts(ids);
        return Result.ok();
    }
}
