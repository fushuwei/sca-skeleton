package io.github.fushuwei.scaskeleton.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.exception.ForbiddenException;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.log.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import io.github.fushuwei.scaskeleton.system.api.request.loginlog.LoginLogPageRequest;
import io.github.fushuwei.scaskeleton.system.api.response.loginlog.LoginLogResponse;
import io.github.fushuwei.scaskeleton.system.service.SysLoginLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 登录日志管理 Controller。
 *
 * @author Fu Wei
 */
@RestController
@RequestMapping("/login-log")
@RequiredArgsConstructor
public class SysLoginLogController {

    private final SysLoginLogService loginLogService;

    // 分页查询登录日志，需 sys:login-log:list
    @GetMapping("/page")
    @RequiresPermission("sys:login-log:list")
    public Result<IPage<LoginLogResponse>> page(@Validated LoginLogPageRequest request) {
        return Result.ok(loginLogService.pageLogs(request));
    }

    // 按 ID 查询登录日志详情，需 sys:login-log:query
    @GetMapping("/{id}")
    @RequiresPermission("sys:login-log:query")
    public Result<LoginLogResponse> getById(@PathVariable("id") String id) {
        return Result.ok(loginLogService.getLogById(id));
    }

    // 批量删除登录日志，仅超级管理员可操作
    @PostMapping("/batch/delete")
    @RequiresPermission("sys:login-log:delete")
    @OperationLog(module = "登录日志", action = "批量删除登录日志")
    public Result<Void> batchDelete(@RequestBody List<String> ids) {
        if (!SecurityUtils.isSuperAdmin()) {
            throw new ForbiddenException("仅超级管理员可批量删除登录日志");
        }
        loginLogService.batchDeleteLogs(ids);
        return Result.ok();
    }

    // 清空全部登录日志，仅超级管理员可操作
    @PostMapping("/clear")
    @RequiresPermission("sys:login-log:delete")
    @OperationLog(module = "登录日志", action = "清空登录日志")
    public Result<Void> clearAll() {
        if (!SecurityUtils.isSuperAdmin()) {
            throw new ForbiddenException("仅超级管理员可清空登录日志");
        }
        loginLogService.clearAllLogs();
        return Result.ok();
    }
}
