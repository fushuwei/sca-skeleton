package io.github.fushuwei.scaskeleton.datasource.controller;

import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.datasource.api.request.query.SqlExecuteRequest;
import io.github.fushuwei.scaskeleton.datasource.api.response.query.SqlExecuteResponse;
import io.github.fushuwei.scaskeleton.datasource.service.QueryService;
import io.github.fushuwei.scaskeleton.log.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据查询 Controller
 * <p>
 * 权限码：sys:datasource:sql-query
 *
 * @author Fu Wei
 */
@Tag(name = "数据查询")
@RestController
@RequestMapping("/query")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    @Operation(summary = "执行 SQL 查询")
    @PostMapping("/execute")
    @RequiresPermission("sys:datasource:sql-query")
    @OperationLog(module = "数据查询", action = "执行 SQL 查询")
    public Result<SqlExecuteResponse> execute(@Validated @RequestBody SqlExecuteRequest request) {
        return Result.ok(queryService.executeSql(request));
    }
}
