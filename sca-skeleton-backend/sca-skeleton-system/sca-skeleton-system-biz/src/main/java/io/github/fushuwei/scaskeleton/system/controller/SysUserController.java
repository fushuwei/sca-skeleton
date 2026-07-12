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
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理 Controller。
 *
 * @author Fu Wei
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    // 获取当前登录用户资料，无需额外权限编码（OAuth2 登录后 SPA 拉取）
    @GetMapping("/profile")
    public Result<UserProfileResponse> profile() {
        return Result.ok(userService.getCurrentProfile());
    }

    // 分页查询当前租户下用户列表（含部门名称、角色名称，排除密码）
    @GetMapping("/page")
    @RequiresPermission("sys:user:list")
    public Result<IPage<UserPageResponse>> page(@Validated UserPageRequest request) {
        String tenantId = SecurityUtils.getTenantId();
        return Result.ok(userService.pageUsers(tenantId, request));
    }

    // 按 ID 查询用户详情，需 sys:user:query
    @GetMapping("/{id}")
    @RequiresPermission("sys:user:query")
    public Result<UserResponse> getById(@PathVariable("id") String id) {
        return Result.ok(userService.getUserById(id));
    }

    // 在当前租户下创建用户，需 sys:user:add
    @PostMapping
    @RequiresPermission("sys:user:add")
    @OperationLog(module = "用户管理", action = "新增用户")
    public Result<Void> create(
            @Validated @RequestBody UserCreateRequest request) {
        userService.createUser(SecurityUtils.getTenantId(), request);
        return Result.ok();
    }

    // 更新当前租户下用户信息，需 sys:user:edit
    @PutMapping
    @RequiresPermission("sys:user:edit")
    @OperationLog(module = "用户管理", action = "编辑用户")
    public Result<Void> update(
            @Validated @RequestBody UserUpdateRequest request) {
        userService.updateUser(SecurityUtils.getTenantId(), request);
        return Result.ok();
    }

    // 删除指定用户，需 sys:user:delete
    @DeleteMapping("/{id}")
    @RequiresPermission("sys:user:delete")
    @OperationLog(module = "用户管理", action = "删除用户")
    public Result<Void> delete(@PathVariable("id") String id) {
        userService.deleteUser(id);
        return Result.ok();
    }

    // 批量删除用户
    @DeleteMapping("/batch")
    @RequiresPermission("sys:user:delete")
    @OperationLog(module = "用户管理", action = "批量删除用户")
    public Result<Void> batchDelete(@RequestBody List<String> ids) {
        userService.batchDeleteUsers(ids);
        return Result.ok();
    }

    // 重置用户登录密码
    @PutMapping("/{id}/password/reset")
    @RequiresPermission("sys:user:reset-password")
    @OperationLog(module = "用户管理", action = "重置密码")
    public Result<Void> resetPassword(@PathVariable("id") String id,
                                          @Validated @RequestBody UserPasswordResetRequest request) {
        userService.resetPassword(id, request.getNewPassword());
        return Result.ok();
    }

    // 变更用户状态（启用/禁用等）
    @PutMapping("/{id}/status")
    @RequiresPermission("sys:user:edit")
    @OperationLog(module = "用户管理", action = "变更用户状态")
    public Result<Void> changeStatus(@PathVariable("id") String id,
                                         @Validated @RequestBody UserStatusChangeRequest request) {
        userService.changeStatus(id, request.getStatus(), request.getReason());
        return Result.ok();
    }

    // 批量变更用户状态
    @PutMapping("/batch/status")
    @RequiresPermission("sys:user:edit")
    @OperationLog(module = "用户管理", action = "批量变更用户状态")
    public Result<Void> batchChangeStatus(@Validated @RequestBody UserBatchStatusRequest request) {
        userService.batchChangeStatus(request.getIds(), request.getStatus(), request.getReason());
        return Result.ok();
    }
}
