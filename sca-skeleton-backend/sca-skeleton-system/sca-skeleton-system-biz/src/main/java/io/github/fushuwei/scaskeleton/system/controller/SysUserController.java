package io.github.fushuwei.scaskeleton.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.log.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.github.fushuwei.scaskeleton.system.api.request.DeleteRequest;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserBatchStatusRequest;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserPasswordResetRequest;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserStatusChangeRequest;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.user.UserProfileResponse;
import io.github.fushuwei.scaskeleton.system.api.response.user.UserResponse;
import io.github.fushuwei.scaskeleton.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "用户管理")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @Operation(summary = "获取当前登录用户基本信息")
    @GetMapping("/profile")
    public Result<UserProfileResponse> profile() {
        return Result.ok(userService.getUserProfile());
    }

    @Operation(summary = "分页查询用户列表")
    @GetMapping("/page")
    @RequiresPermission("sys:user:list")
    public Result<IPage<UserResponse>> page(@Validated UserPageRequest request) {
        return Result.ok(userService.pageUsers(request));
    }

    @Operation(summary = "根据 ID 查询用户详情")
    @GetMapping("/{id}")
    @RequiresPermission("sys:user:list")
    public Result<UserResponse> getById(@PathVariable String id) {
        return Result.ok(userService.getUserById(id));
    }

    @Operation(summary = "新增用户")
    @PostMapping("/create")
    @RequiresPermission("sys:user:add")
    @OperationLog(module = "用户管理", action = "新增用户")
    public Result<Void> create(@Validated @RequestBody UserCreateRequest request) {
        userService.createUser(request);
        return Result.ok();
    }

    @Operation(summary = "编辑用户")
    @PostMapping("/update")
    @RequiresPermission("sys:user:edit")
    @OperationLog(module = "用户管理", action = "编辑用户")
    public Result<Void> update(@Validated @RequestBody UserUpdateRequest request) {
        userService.updateUser(request);
        return Result.ok();
    }

    @Operation(summary = "删除用户")
    @PostMapping("/delete")
    @RequiresPermission("sys:user:delete")
    @OperationLog(module = "用户管理", action = "删除用户")
    public Result<Void> delete(@Validated @RequestBody DeleteRequest request) {
        userService.deleteUser(request.getId());
        return Result.ok();
    }

    @Operation(summary = "批量删除用户")
    @PostMapping("/batch/delete")
    @RequiresPermission("sys:user:delete")
    @OperationLog(module = "用户管理", action = "批量删除用户")
    public Result<Void> batchDelete(@RequestBody List<String> ids) {
        userService.batchDeleteUsers(ids);
        return Result.ok();
    }

    @Operation(summary = "重置密码")
    @PostMapping("/reset-password")
    @RequiresPermission("sys:user:reset-password")
    @OperationLog(module = "用户管理", action = "重置密码")
    public Result<Void> resetPassword(@Validated @RequestBody UserPasswordResetRequest request) {
        userService.resetPassword(request);
        return Result.ok();
    }

    @Operation(summary = "变更用户状态")
    @PostMapping("/change-status")
    @RequiresPermission("sys:user:edit")
    @OperationLog(module = "用户管理", action = "变更用户状态")
    public Result<Void> changeStatus(@Validated @RequestBody UserStatusChangeRequest request) {
        userService.changeStatus(request);
        return Result.ok();
    }

    @Operation(summary = "批量变更用户状态")
    @PostMapping("/batch/change-status")
    @RequiresPermission("sys:user:edit")
    @OperationLog(module = "用户管理", action = "批量变更用户状态")
    public Result<Void> batchChangeStatus(@Validated @RequestBody UserBatchStatusRequest request) {
        userService.batchChangeStatus(request);
        return Result.ok();
    }
}
