package io.github.fushuwei.scaskeleton.system.api.controller;

import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import io.github.fushuwei.scaskeleton.system.api.dto.dept.DeptSaveRequest;
import io.github.fushuwei.scaskeleton.system.application.service.SysDeptService;
import io.github.fushuwei.scaskeleton.system.infrastructure.entity.SysDept;
import lombok.RequiredArgsConstructor;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
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

    @GetMapping("/list")
    @RequiresPermission("sys:dept:list")
    public Result<List<SysDept>> list() {
        return Result.ok(deptService.listDepts(SecurityUtils.getTenantId()));
    }

    @GetMapping("/{id}")
    @RequiresPermission("sys:dept:query")
    public Result<SysDept> getById(@PathVariable String id) {
        return Result.ok(deptService.getDeptById(id));
    }

    @PostMapping
    @RequiresPermission("sys:dept:add")
    public Result<Void> create(@Validated @RequestBody DeptSaveRequest request) {
        deptService.createDept(SecurityUtils.getTenantId(), request);
        return Result.ok();
    }

    @PutMapping
    @RequiresPermission("sys:dept:edit")
    public Result<Void> update(@Validated @RequestBody DeptSaveRequest request) {
        deptService.updateDept(SecurityUtils.getTenantId(), request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("sys:dept:delete")
    public Result<Void> delete(@PathVariable String id) {
        deptService.deleteDept(id);
        return Result.ok();
    }
}
