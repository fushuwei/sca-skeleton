package io.github.fushuwei.scaskeleton.system.api.controller;

import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import io.github.fushuwei.scaskeleton.web.response.ApiResponse;
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
    public ApiResponse<List<SysDept>> list() {
        return ApiResponse.success(deptService.listDepts(SecurityUtils.getTenantId()));
    }

    @GetMapping("/{id}")
    @RequiresPermission("sys:dept:query")
    public ApiResponse<SysDept> getById(@PathVariable String id) {
        return ApiResponse.success(deptService.getDeptById(id));
    }

    @PostMapping
    @RequiresPermission("sys:dept:add")
    public ApiResponse<Void> create(@Validated @RequestBody DeptSaveRequest request) {
        deptService.createDept(SecurityUtils.getTenantId(), request);
        return ApiResponse.success();
    }

    @PutMapping
    @RequiresPermission("sys:dept:edit")
    public ApiResponse<Void> update(@Validated @RequestBody DeptSaveRequest request) {
        deptService.updateDept(SecurityUtils.getTenantId(), request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("sys:dept:delete")
    public ApiResponse<Void> delete(@PathVariable String id) {
        deptService.deleteDept(id);
        return ApiResponse.success();
    }
}
