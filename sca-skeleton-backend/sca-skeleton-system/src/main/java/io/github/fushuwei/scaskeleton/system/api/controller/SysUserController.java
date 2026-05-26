package io.github.fushuwei.scaskeleton.system.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.core.validation.ValidGroup;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import io.github.fushuwei.scaskeleton.system.api.dto.user.UserPageRequest;
import io.github.fushuwei.scaskeleton.system.api.dto.user.UserProfileVO;
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

    // 获取当前登录用户资料，无需额外权限码（OAuth2 登录后 SPA 拉取）
    @GetMapping("/profile")
    public Result<UserProfileVO> profile() {
        return Result.ok(userService.getCurrentProfile());
    }

    // 分页查询当前租户下用户列表，需 sys:user:list；租户 ID 从 SecurityContext 读取
    @GetMapping("/page")
    @RequiresPermission("sys:user:list")
    public Result<IPage<SysUser>> page(@Validated UserPageRequest request) {
        String tenantId = SecurityUtils.getTenantId();
        return Result.ok(userService.pageUsers(tenantId, request));
    }

    // 按 ID 查询用户详情，需 sys:user:query
    @GetMapping("/{id}")
    @RequiresPermission("sys:user:query")
    public Result<SysUser> getById(@PathVariable String id) {
        return Result.ok(userService.getUserById(id));
    }

    // 在当前租户下创建用户，需 sys:user:add
    @PostMapping
    @RequiresPermission("sys:user:add")
    public Result<Void> create(
            @Validated(ValidGroup.Create.class) @RequestBody UserSaveRequest request) {
        userService.createUser(SecurityUtils.getTenantId(), request);
        return Result.ok();
    }

    // 更新当前租户下用户信息，需 sys:user:edit
    @PutMapping
    @RequiresPermission("sys:user:edit")
    public Result<Void> update(
            @Validated(ValidGroup.Update.class) @RequestBody UserSaveRequest request) {
        userService.updateUser(SecurityUtils.getTenantId(), request);
        return Result.ok();
    }

    // 删除指定用户，需 sys:user:delete
    @DeleteMapping("/{id}")
    @RequiresPermission("sys:user:delete")
    public Result<Void> delete(@PathVariable String id) {
        userService.deleteUser(id);
        return Result.ok();
    }

    // 重置用户登录密码，需 sys:user:reset-password
    @PutMapping("/{id}/password/reset")
    @RequiresPermission("sys:user:reset-password")
    public Result<Void> resetPassword(@PathVariable String id,
                                          @RequestParam String newPassword) {
        userService.resetPassword(id, newPassword);
        return Result.ok();
    }

    // 变更用户状态（启用/禁用等），需 sys:user:edit
    @PutMapping("/{id}/status")
    @RequiresPermission("sys:user:edit")
    public Result<Void> changeStatus(@PathVariable String id,
                                         @RequestParam String status,
                                         @RequestParam(required = false) String reason) {
        userService.changeStatus(id, status, reason);
        return Result.ok();
    }
}
