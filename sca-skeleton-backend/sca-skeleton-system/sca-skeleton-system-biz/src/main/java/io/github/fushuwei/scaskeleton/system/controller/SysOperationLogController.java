package io.github.fushuwei.scaskeleton.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.logging.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.github.fushuwei.scaskeleton.system.api.request.operationlog.OperationLogPageRequest;
import io.github.fushuwei.scaskeleton.system.api.response.operationlog.OperationLogResponse;
import io.github.fushuwei.scaskeleton.system.service.SysOperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 操作日志管理 Controller。
 *
 * @author Fu Wei
 */
@RestController
@RequestMapping("/operation-log")
@RequiredArgsConstructor
public class SysOperationLogController {

    private final SysOperationLogService operationLogService;

    // 分页查询操作日志，需 sys:operation-log:list
    @GetMapping("/page")
    @RequiresPermission("sys:operation-log:list")
    public Result<IPage<OperationLogResponse>> page(@Validated OperationLogPageRequest request) {
        return Result.ok(operationLogService.pageLogs(request));
    }

    // 按 ID 查询操作日志详情，需 sys:operation-log:query
    @GetMapping("/{id}")
    @RequiresPermission("sys:operation-log:query")
    public Result<OperationLogResponse> getById(@PathVariable("id") String id) {
        return Result.ok(operationLogService.getLogById(id));
    }

    // 批量删除操作日志，需 sys:operation-log:delete
    @DeleteMapping("/batch")
    @RequiresPermission("sys:operation-log:delete")
    @OperationLog(module = "操作日志", action = "批量删除日志", logArgs = false)
    public Result<Void> batchDelete(@RequestBody List<String> ids) {
        operationLogService.batchDeleteLogs(ids);
        return Result.ok();
    }

    // 清空全部操作日志，需 sys:operation-log:delete
    @DeleteMapping("/clear")
    @RequiresPermission("sys:operation-log:delete")
    @OperationLog(module = "操作日志", action = "清空日志")
    public Result<Void> clearAll() {
        operationLogService.clearAllLogs();
        return Result.ok();
    }
}
