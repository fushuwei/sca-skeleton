package io.github.fushuwei.scaskeleton.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.log.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserBatchStatusRequest;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserPasswordResetRequest;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserStatusChangeRequest;
import io.github.fushuwei.scaskeleton.system.api.response.user.UserPageResponse;
import io.github.fushuwei.scaskeleton.system.api.response.user.UserProfileResponse;
import io.github.fushuwei.scaskeleton.system.api.response.user.UserResponse;
import io.github.fushuwei.scaskeleton.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理 Controller
 *
 * @author Fu Wei
 */
@Tag(name = "用户管理", description = "用户的增删改查、密码重置、状态变更等管理操作")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @Operation(summary = "获取当前登录用户基本信息", description = "根据 Bearer Token 自省结果查询登录用户的基本信息")
    @GetMapping("/profile")
    public Result<UserProfileResponse> profile() {
        return Result.ok(userService.getCurrentProfile());
    }

    @Operation(summary = "分页查询用户列表", description = "分页查询当前租户下的用户列表，支持关键词搜索、状态筛选、部门筛选与排序")
    @GetMapping("/page")
    @RequiresPermission("sys:user:list")
    public Result<IPage<UserPageResponse>> page(@Validated UserPageRequest request) {
        String tenantId = SecurityUtils.getTenantId();
        return Result.ok(userService.pageUsers(tenantId, request));
    }

    @Operation(summary = "查询用户详情", description = "按 ID 查询用户完整信息（排除密码等敏感字段）")
    @Parameter(name = "id", description = "用户 ID", required = true)
    @GetMapping("/{id}")
    @RequiresPermission("sys:user:query")
    public Result<UserResponse> getById(@PathVariable("id") String id) {
        return Result.ok(userService.getUserById(id));
    }

    @Operation(summary = "创建用户", description = "在当前租户下创建新用户，可同时分配部门、岗位与角色")
    @PostMapping("/create")
    @RequiresPermission("sys:user:add")
    @OperationLog(module = "用户管理", action = "新增用户")
    public Result<Void> create(
            @Validated @RequestBody UserCreateRequest request) {
        userService.createUser(SecurityUtils.getTenantId(), request);
        return Result.ok();
    }

    @Operation(summary = "编辑用户", description = "更新用户信息，用户名不可修改；密码留空表示不修改")
    @PostMapping("/update")
    @RequiresPermission("sys:user:edit")
    @OperationLog(module = "用户管理", action = "编辑用户")
    public Result<Void> update(
            @Validated @RequestBody UserUpdateRequest request) {
        userService.updateUser(SecurityUtils.getTenantId(), request);
        return Result.ok();
    }

    @Operation(summary = "删除用户", description = "根据用户 ID 删除指定用户")
    @PostMapping("/delete")
    @RequiresPermission("sys:user:delete")
    @OperationLog(module = "用户管理", action = "删除用户")
    public Result<Void> delete(@RequestBody String id) {
        userService.deleteUser(id);
        return Result.ok();
    }

    @Operation(summary = "批量删除用户", description = "根据用户 ID 列表批量删除用户")
    @PostMapping("/batch/delete")
    @RequiresPermission("sys:user:delete")
    @OperationLog(module = "用户管理", action = "批量删除用户")
    public Result<Void> batchDelete(@RequestBody List<String> ids) {
        userService.batchDeleteUsers(ids);
        return Result.ok();
    }

    @Operation(summary = "重置用户密码", description = "管理员重置指定用户的登录密码")
    @PostMapping("/reset-password")
    @RequiresPermission("sys:user:reset-password")
    @OperationLog(module = "用户管理", action = "重置密码")
    public Result<Void> resetPassword(@Validated @RequestBody UserPasswordResetRequest request) {
        userService.resetPassword(request.getId(), request.getNewPassword());
        return Result.ok();
    }

    @Operation(summary = "变更用户状态", description = "启用、禁用、锁定、冻结等用户状态变更，需记录变更原因")
    @PostMapping("/change-status")
    @RequiresPermission("sys:user:edit")
    @OperationLog(module = "用户管理", action = "变更用户状态")
    public Result<Void> changeStatus(@Validated @RequestBody UserStatusChangeRequest request) {
        userService.changeStatus(request.getId(), request.getStatus(), request.getReason());
        return Result.ok();
    }

    @Operation(summary = "批量变更用户状态", description = "批量启用、禁用、锁定、冻结等用户状态变更")
    @PostMapping("/batch/change-status")
    @RequiresPermission("sys:user:edit")
    @OperationLog(module = "用户管理", action = "批量变更用户状态")
    public Result<Void> batchChangeStatus(@Validated @RequestBody UserBatchStatusRequest request) {
        userService.batchChangeStatus(request.getIds(), request.getStatus(), request.getReason());
        return Result.ok();
    }
}
