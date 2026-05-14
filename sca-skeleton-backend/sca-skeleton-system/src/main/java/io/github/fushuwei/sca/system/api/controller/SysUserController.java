package io.github.fushuwei.sca.system.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.validation.ValidGroup;
import io.github.fushuwei.sca.starter.security.context.SecurityUtils;
import io.github.fushuwei.sca.starter.web.response.ApiResponse;
import io.github.fushuwei.sca.system.api.dto.user.UserPageRequest;
import io.github.fushuwei.sca.system.api.dto.user.UserSaveRequest;
import io.github.fushuwei.sca.system.application.service.SysUserService;
import io.github.fushuwei.sca.system.infrastructure.entity.SysUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAuthority('sys:user:list')")
    public ApiResponse<IPage<SysUser>> page(@Validated UserPageRequest request) {
        // 多租户场景下从当前认证主体读取租户标识，避免越权
        String tenantId = SecurityUtils.getTenantId();
        return ApiResponse.success(userService.pageUsers(tenantId, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:user:query')")
    public ApiResponse<SysUser> getById(@PathVariable String id) {
        return ApiResponse.success(userService.getUserById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sys:user:add')")
    public ApiResponse<Void> create(
            @Validated(ValidGroup.Create.class) @RequestBody UserSaveRequest request) {
        userService.createUser(SecurityUtils.getTenantId(), request);
        return ApiResponse.success();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('sys:user:edit')")
    public ApiResponse<Void> update(
            @Validated(ValidGroup.Update.class) @RequestBody UserSaveRequest request) {
        userService.updateUser(SecurityUtils.getTenantId(), request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sys:user:delete')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        userService.deleteUser(id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/password/reset")
    @PreAuthorize("hasAuthority('sys:user:reset-password')")
    public ApiResponse<Void> resetPassword(@PathVariable String id,
                                          @RequestParam String newPassword) {
        userService.resetPassword(id, newPassword);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('sys:user:edit')")
    public ApiResponse<Void> changeStatus(@PathVariable String id,
                                         @RequestParam String status,
                                         @RequestParam(required = false) String reason) {
        userService.changeStatus(id, status, reason);
        return ApiResponse.success();
    }
}
