package io.github.fushuwei.scaskeleton.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.log.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import io.github.fushuwei.scaskeleton.system.api.request.dept.DeptCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dept.DeptPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dept.DeptUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.dept.DeptResponse;
import io.github.fushuwei.scaskeleton.system.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理 Controller。
 *
 * @author Fu Wei
 */
@RestController
@RequestMapping("/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService deptService;

    // 查询当前租户下部门列表，需 sys:dept:list
    @GetMapping("/list")
    @RequiresPermission("sys:dept:list")
    public Result<List<DeptResponse>> list() {
        return Result.ok(deptService.listDepts(SecurityUtils.getTenantId()));
    }

    // 分页查询当前租户下部门列表，需 sys:dept:list
    @GetMapping("/page")
    @RequiresPermission("sys:dept:list")
    public Result<IPage<DeptResponse>> page(@Validated DeptPageRequest request) {
        return Result.ok(deptService.pageDepts(SecurityUtils.getTenantId(), request));
    }

    // 按 ID 查询部门详情，需 sys:dept:query
    @GetMapping("/{id}")
    @RequiresPermission("sys:dept:query")
    public Result<DeptResponse> getById(@PathVariable("id") String id) {
        return Result.ok(deptService.getDeptById(id));
    }

    // 在当前租户下创建部门，需 sys:dept:add
    @PostMapping("/create")
    @RequiresPermission("sys:dept:add")
    @OperationLog(module = "部门管理", action = "添加部门")
    public Result<Void> create(@Validated @RequestBody DeptCreateRequest request) {
        deptService.createDept(SecurityUtils.getTenantId(), request);
        return Result.ok();
    }

    // 更新当前租户下部门信息，需 sys:dept:edit
    @PostMapping("/update")
    @RequiresPermission("sys:dept:edit")
    @OperationLog(module = "部门管理", action = "编辑部门")
    public Result<Void> update(@Validated @RequestBody DeptUpdateRequest request) {
        deptService.updateDept(SecurityUtils.getTenantId(), request);
        return Result.ok();
    }

    // 删除指定部门，需 sys:dept:delete
    @PostMapping("/{id}/delete")
    @RequiresPermission("sys:dept:delete")
    @OperationLog(module = "部门管理", action = "删除部门")
    public Result<Void> delete(@PathVariable("id") String id) {
        deptService.deleteDept(id);
        return Result.ok();
    }

    // 批量删除部门
    @PostMapping("/batch/delete")
    @RequiresPermission("sys:dept:delete")
    @OperationLog(module = "部门管理", action = "批量删除部门")
    public Result<Void> batchDelete(@RequestBody List<String> ids) {
        deptService.batchDeleteDepts(ids);
        return Result.ok();
    }
}
