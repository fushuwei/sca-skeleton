package io.github.fushuwei.scaskeleton.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.log.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.github.fushuwei.scaskeleton.system.api.request.DeleteRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dict.DictDataCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dict.DictDataPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dict.DictDataUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.dict.DictDataResponse;
import io.github.fushuwei.scaskeleton.system.service.SysDictDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典数据管理 Controller
 *
 * @author Fu Wei
 */
@Tag(name = "字典数据管理")
@RestController
@RequestMapping("/dict/data")
@RequiredArgsConstructor
public class SysDictDataController {

    private final SysDictDataService dictDataService;

    @Operation(summary = "分页查询字典数据列表")
    @GetMapping("/page")
    @RequiresPermission("sys:dict:list")
    public Result<IPage<DictDataResponse>> page(@Validated DictDataPageRequest request) {
        return Result.ok(dictDataService.pageDictData(request));
    }

    @Operation(summary = "根据 ID 查询字典数据详情")
    @GetMapping("/{id}")
    @RequiresPermission("sys:dict:list")
    public Result<DictDataResponse> getById(@PathVariable String id) {
        return Result.ok(dictDataService.getDictDataById(id));
    }

    @Operation(summary = "新增字典数据")
    @PostMapping("/create")
    @RequiresPermission("sys:dict:add")
    @OperationLog(module = "字典数据管理", action = "新增字典数据")
    public Result<Void> create(@Validated @RequestBody DictDataCreateRequest request) {
        dictDataService.createDictData(request);
        return Result.ok();
    }

    @Operation(summary = "编辑字典数据")
    @PostMapping("/update")
    @RequiresPermission("sys:dict:edit")
    @OperationLog(module = "字典数据管理", action = "编辑字典数据")
    public Result<Void> update(@Validated @RequestBody DictDataUpdateRequest request) {
        dictDataService.updateDictData(request);
        return Result.ok();
    }

    @Operation(summary = "删除字典数据")
    @PostMapping("/delete")
    @RequiresPermission("sys:dict:delete")
    @OperationLog(module = "字典数据管理", action = "删除字典数据")
    public Result<Void> delete(@Validated @RequestBody DeleteRequest request) {
        dictDataService.deleteDictData(request.getId());
        return Result.ok();
    }

    @Operation(summary = "批量删除字典数据")
    @PostMapping("/batch/delete")
    @RequiresPermission("sys:dict:delete")
    @OperationLog(module = "字典数据管理", action = "批量删除字典数据")
    public Result<Void> batchDelete(@RequestBody List<String> ids) {
        dictDataService.batchDeleteDictData(ids);
        return Result.ok();
    }
}
