package io.github.fushuwei.sca.system.api.controller;

import io.github.fushuwei.sca.starter.security.context.SecurityUtils;
import io.github.fushuwei.sca.starter.web.response.ApiResponse;
import io.github.fushuwei.sca.system.api.dto.dept.DeptSaveRequest;
import io.github.fushuwei.sca.system.application.service.SysDeptService;
import io.github.fushuwei.sca.system.infrastructure.entity.SysDept;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAuthority('sys:dept:list')")
    public ApiResponse<List<SysDept>> list() {
        return ApiResult.success(deptService.listDepts(SecurityUtils.getTenantId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:dept:query')")
    public ApiResponse<SysDept> getById(@PathVariable String id) {
        return ApiResult.success(deptService.getDeptById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sys:dept:add')")
    public ApiResponse<Void> create(@Validated @RequestBody DeptSaveRequest request) {
        deptService.createDept(SecurityUtils.getTenantId(), request);
        return ApiResult.success();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('sys:dept:edit')")
    public ApiResponse<Void> update(@Validated @RequestBody DeptSaveRequest request) {
        deptService.updateDept(SecurityUtils.getTenantId(), request);
        return ApiResult.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:dept:delete')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        deptService.deleteDept(id);
        return ApiResult.success();
    }
}
