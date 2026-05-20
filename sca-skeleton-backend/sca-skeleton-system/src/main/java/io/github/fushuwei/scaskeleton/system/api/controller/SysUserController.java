package io.github.fushuwei.scaskeleton.system.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.core.validation.ValidGroup;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import io.github.fushuwei.scaskeleton.system.api.dto.user.UserPageRequest;
import io.github.fushuwei.scaskeleton.system.api.dto.user.UserSaveRequest;
import io.github.fushuwei.scaskeleton.system.application.service.SysUserService;
import io.github.fushuwei.scaskeleton.system.infrastructure.entity.SysUser;
import lombok.RequiredArgsConstructor;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/page")
    @RequiresPermission("sys:user:list")
    public Result<IPage<SysUser>> page(@Validated UserPageRequest request) {
        // 多租户场景下从当前认证主体读取租户标识，避免越权
        String tenantId = SecurityUtils.getTenantId();
        return Result.ok(userService.pageUsers(tenantId, request));
    }

    @GetMapping("/{id}")
    @RequiresPermission("sys:user:query")
    public Result<SysUser> getById(@PathVariable String id) {
        return Result.ok(userService.getUserById(id));
    }

    @PostMapping
    @RequiresPermission("sys:user:add")
    public Result<Void> create(
            @Validated(ValidGroup.Create.class) @RequestBody UserSaveRequest request) {
        userService.createUser(SecurityUtils.getTenantId(), request);
        return Result.ok();
    }

    @PutMapping
    @RequiresPermission("sys:user:edit")
    public Result<Void> update(
            @Validated(ValidGroup.Update.class) @RequestBody UserSaveRequest request) {
        userService.updateUser(SecurityUtils.getTenantId(), request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("sys:user:delete")
    public Result<Void> delete(@PathVariable String id) {
        userService.deleteUser(id);
        return Result.ok();
    }

    @PutMapping("/{id}/password/reset")
    @RequiresPermission("sys:user:reset-password")
    public Result<Void> resetPassword(@PathVariable String id,
                                          @RequestParam String newPassword) {
        userService.resetPassword(id, newPassword);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    @RequiresPermission("sys:user:edit")
    public Result<Void> changeStatus(@PathVariable String id,
                                         @RequestParam String status,
                                         @RequestParam(required = false) String reason) {
        userService.changeStatus(id, status, reason);
        return Result.ok();
    }
}
