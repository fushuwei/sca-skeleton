package io.github.fushuwei.scaskeleton.system.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.core.validation.ValidGroup;
import io.github.fushuwei.scaskeleton.logging.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import io.github.fushuwei.scaskeleton.system.api.dto.user.UserPageRequest;
import io.github.fushuwei.scaskeleton.system.api.dto.user.UserPageVO;
import io.github.fushuwei.scaskeleton.system.api.dto.user.UserProfileVO;
import io.github.fushuwei.scaskeleton.system.api.dto.user.UserSaveRequest;
import io.github.fushuwei.scaskeleton.system.application.service.SysUserService;
import io.github.fushuwei.scaskeleton.system.infrastructure.entity.SysUser;
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
    public Result<UserProfileVO> profile() {
        return Result.ok(userService.getCurrentProfile());
    }

    // 分页查询当前租户下用户列表（含部门名称、角色名称，排除密码）
    @GetMapping("/page")
    @RequiresPermission("sys:user:list")
    public Result<IPage<UserPageVO>> page(@Validated UserPageRequest request) {
        String tenantId = SecurityUtils.getTenantId();
        return Result.ok(userService.pageUsers(tenantId, request));
    }

    // 按 ID 查询用户详情，需 sys:user:query
    @GetMapping("/{id}")
    @RequiresPermission("sys:user:query")
    public Result<SysUser> getById(@PathVariable("id") String id) {
        return Result.ok(userService.getUserById(id));
    }

    // 在当前租户下创建用户，需 sys:user:add
    @PostMapping
    @RequiresPermission("sys:user:add")
    @OperationLog(module = "用户管理", action = "新增用户", logArgs = false)
    public Result<Void> create(
            @Validated(ValidGroup.Create.class) @RequestBody UserSaveRequest request) {
        userService.createUser(SecurityUtils.getTenantId(), request);
        return Result.ok();
    }

    // 更新当前租户下用户信息，需 sys:user:edit
    @PutMapping
    @RequiresPermission("sys:user:edit")
    @OperationLog(module = "用户管理", action = "编辑用户", logArgs = false)
    public Result<Void> update(
            @Validated(ValidGroup.Update.class) @RequestBody UserSaveRequest request) {
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
    @OperationLog(module = "用户管理", action = "重置密码", logArgs = false)
    public Result<Void> resetPassword(@PathVariable("id") String id,
                                          @RequestParam("newPassword") String newPassword) {
        userService.resetPassword(id, newPassword);
        return Result.ok();
    }

    // 变更用户状态（启用/禁用等）
    @PutMapping("/{id}/status")
    @RequiresPermission("sys:user:edit")
    @OperationLog(module = "用户管理", action = "变更用户状态")
    public Result<Void> changeStatus(@PathVariable("id") String id,
                                         @RequestParam("status") String status,
                                         @RequestParam(value = "reason", required = false) String reason) {
        userService.changeStatus(id, status, reason);
        return Result.ok();
    }

    // 批量变更用户状态
    @PutMapping("/batch/status")
    @RequiresPermission("sys:user:edit")
    @OperationLog(module = "用户管理", action = "批量变更用户状态")
    public Result<Void> batchChangeStatus(@RequestBody BatchStatusRequest request) {
        userService.batchChangeStatus(request.getIds(), request.getStatus(), request.getReason());
        return Result.ok();
    }

    /**
     * 批量状态变更请求体。
     */
    @lombok.Data
    public static class BatchStatusRequest {
        private List<String> ids;
        private String status;
        private String reason;
    }
}
