package io.github.fushuwei.scaskeleton.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.log.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.github.fushuwei.scaskeleton.system.api.request.loginlog.LoginLogPageRequest;
import io.github.fushuwei.scaskeleton.system.api.response.loginlog.LoginLogResponse;
import io.github.fushuwei.scaskeleton.system.service.SysLoginLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 登录日志管理 Controller
 *
 * @author Fu Wei
 */
@Tag(name = "登录日志")
@RestController
@RequestMapping("/login-log")
@RequiredArgsConstructor
public class SysLoginLogController {

    private final SysLoginLogService loginLogService;

    @Operation(summary = "分页查询登录日志")
    @GetMapping("/page")
    @RequiresPermission("sys:login-log:list")
    public Result<IPage<LoginLogResponse>> page(@Validated LoginLogPageRequest request) {
        return Result.ok(loginLogService.pageLogs(request));
    }

    @Operation(summary = "根据 ID 查询登录日志详情")
    @GetMapping("/{id}")
    @RequiresPermission("sys:login-log:query")
    public Result<LoginLogResponse> getById(@PathVariable String id) {
        return Result.ok(loginLogService.getLogById(id));
    }

    @Operation(summary = "批量删除登录日志")
    @PostMapping("/batch/delete")
    @RequiresPermission("sys:login-log:delete")
    @OperationLog(module = "登录日志", action = "批量删除登录日志")
    public Result<Void> batchDelete(@RequestBody List<String> ids) {
        loginLogService.batchDeleteLogs(ids);
        return Result.ok();
    }

    @Operation(summary = "清空登录日志")
    @PostMapping("/clear")
    @RequiresPermission("sys:login-log:delete")
    @OperationLog(module = "登录日志", action = "清空登录日志")
    public Result<Void> clearAll() {
        loginLogService.clearAllLogs();
        return Result.ok();
    }
}
