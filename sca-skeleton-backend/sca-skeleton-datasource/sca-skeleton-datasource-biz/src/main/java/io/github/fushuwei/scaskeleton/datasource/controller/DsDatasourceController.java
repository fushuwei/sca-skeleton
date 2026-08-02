package io.github.fushuwei.scaskeleton.datasource.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.datasource.api.enums.DbType;
import io.github.fushuwei.scaskeleton.datasource.api.request.DeleteRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.datasource.DatasourceCreateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.datasource.DatasourcePageRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.datasource.DatasourceUpdateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.response.datasource.DatasourceResponse;
import io.github.fushuwei.scaskeleton.datasource.service.DsDatasourceService;
import io.github.fushuwei.scaskeleton.log.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据源管理 Controller
 * <p>
 * 权限码：sys:datasource:list/add/edit/delete
 *
 * @author Fu Wei
 */
@Tag(name = "数据源管理")
@RestController
@RequestMapping("/ds/datasource")
@RequiredArgsConstructor
public class DsDatasourceController {

    private final DsDatasourceService datasourceService;

    @Operation(summary = "分页查询数据源列表")
    @GetMapping("/page")
    @RequiresPermission("sys:datasource:list")
    public Result<IPage<DatasourceResponse>> page(@Validated DatasourcePageRequest request) {
        return Result.ok(datasourceService.pageDatasources(request));
    }

    @Operation(summary = "根据 ID 查询数据源详情")
    @GetMapping("/{id}")
    @RequiresPermission("sys:datasource:list")
    public Result<DatasourceResponse> getById(@PathVariable String id) {
        return Result.ok(datasourceService.getDatasourceById(id));
    }

    @Operation(summary = "新增数据源")
    @PostMapping("/create")
    @RequiresPermission("sys:datasource:add")
    @OperationLog(module = "数据源管理", action = "新增数据源")
    public Result<Void> create(@Validated @RequestBody DatasourceCreateRequest request) {
        datasourceService.createDatasource(request);
        return Result.ok();
    }

    @Operation(summary = "编辑数据源")
    @PostMapping("/update")
    @RequiresPermission("sys:datasource:edit")
    @OperationLog(module = "数据源管理", action = "编辑数据源")
    public Result<Void> update(@Validated @RequestBody DatasourceUpdateRequest request) {
        datasourceService.updateDatasource(request);
        return Result.ok();
    }

    @Operation(summary = "删除数据源")
    @PostMapping("/delete")
    @RequiresPermission("sys:datasource:delete")
    @OperationLog(module = "数据源管理", action = "删除数据源")
    public Result<Void> delete(@Validated @RequestBody DeleteRequest request) {
        datasourceService.deleteDatasource(request.getId());
        return Result.ok();
    }

    @Operation(summary = "测试数据源连接")
    @PostMapping("/test")
    @RequiresPermission("sys:datasource:list")
    @OperationLog(module = "数据源管理", action = "测试连接")
    public Result<DatasourceResponse> test(@RequestParam String id) {
        return Result.ok(datasourceService.testConnection(id));
    }

    @Operation(summary = "启用数据源")
    @PostMapping("/{id}/enable")
    @RequiresPermission("sys:datasource:edit")
    @OperationLog(module = "数据源管理", action = "启用数据源")
    public Result<Void> enable(@PathVariable String id) {
        datasourceService.changeEnabled(id, 1);
        return Result.ok();
    }

    @Operation(summary = "禁用数据源")
    @PostMapping("/{id}/disable")
    @RequiresPermission("sys:datasource:edit")
    @OperationLog(module = "数据源管理", action = "禁用数据源")
    public Result<Void> disable(@PathVariable String id) {
        datasourceService.changeEnabled(id, 0);
        return Result.ok();
    }

    @Operation(summary = "获取数据库类型下拉")
    @GetMapping("/db-types")
    @RequiresPermission("sys:datasource:list")
    public Result<List<DbType>> dbTypes() {
        return Result.ok(datasourceService.listDbTypes());
    }

    @Operation(summary = "查询数据源下的数据库列表")
    @GetMapping("/{id}/databases")
    @RequiresPermission("sys:datasource:list")
    public Result<List<String>> databases(@PathVariable String id) {
        return Result.ok(datasourceService.listDatabases(id));
    }

    @Operation(summary = "查询数据源下的表列表")
    @GetMapping("/{id}/tables")
    @RequiresPermission("sys:datasource:list")
    public Result<List<String>> tables(@PathVariable String id, @RequestParam(required = false) String database) {
        return Result.ok(datasourceService.listTables(id, database));
    }

    @Operation(summary = "查询表字段列表")
    @GetMapping("/{id}/columns")
    @RequiresPermission("sys:datasource:list")
    public Result<List<String>> columns(@PathVariable String id, @RequestParam String table) {
        return Result.ok(datasourceService.listColumns(id, table));
    }
}
