package io.github.fushuwei.scaskeleton.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.log.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.github.fushuwei.scaskeleton.system.api.request.operationlog.OperationLogPageRequest;
import io.github.fushuwei.scaskeleton.system.api.response.operationlog.OperationLogResponse;
import io.github.fushuwei.scaskeleton.system.service.SysOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 操作日志管理 Controller
 *
 * @author Fu Wei
 */
@Tag(name = "操作日志")
@RestController
@RequestMapping("/operation-log")
@RequiredArgsConstructor
public class SysOperationLogController {

    private final SysOperationLogService operationLogService;

    @Operation(summary = "分页查询操作日志")
    @GetMapping("/page")
    @RequiresPermission("sys:operation-log:list")
    public Result<IPage<OperationLogResponse>> page(@Validated OperationLogPageRequest request) {
        return Result.ok(operationLogService.pageLogs(request));
    }

    @Operation(summary = "根据 ID 查询操作日志详情")
    @GetMapping("/{id}")
    @RequiresPermission("sys:operation-log:list")
    public Result<OperationLogResponse> getById(@PathVariable String id) {
        return Result.ok(operationLogService.getLogById(id));
    }

    @Operation(summary = "批量删除操作日志")
    @PostMapping("/batch/delete")
    @RequiresPermission("sys:operation-log:delete")
    @OperationLog(module = "操作日志", action = "批量删除日志")
    public Result<Void> batchDelete(@RequestBody List<String> ids) {
        operationLogService.batchDeleteLogs(ids);
        return Result.ok();
    }

    @Operation(summary = "清空操作日志")
    @PostMapping("/clear")
    @RequiresPermission("sys:operation-log:delete")
    @OperationLog(module = "操作日志", action = "清空日志")
    public Result<Void> clearAll() {
        operationLogService.clearAllLogs();
        return Result.ok();
    }
}
