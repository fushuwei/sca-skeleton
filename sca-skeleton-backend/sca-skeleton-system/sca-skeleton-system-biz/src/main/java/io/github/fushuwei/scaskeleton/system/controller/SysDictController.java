package io.github.fushuwei.scaskeleton.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.log.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.github.fushuwei.scaskeleton.system.api.request.DeleteRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dict.DictCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dict.DictPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.dict.DictUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.dict.DictResponse;
import io.github.fushuwei.scaskeleton.system.service.SysDictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典管理 Controller
 *
 * @author Fu Wei
 */
@Tag(name = "字典管理")
@RestController
@RequestMapping("/dict")
@RequiredArgsConstructor
public class SysDictController {

    private final SysDictService dictService;

    @Operation(summary = "查询字典列表")
    @GetMapping("/list")
    @RequiresPermission("sys:dict:list")
    public Result<List<DictResponse>> list() {
        return Result.ok(dictService.listDicts());
    }

    @Operation(summary = "分页查询字典列表")
    @GetMapping("/page")
    @RequiresPermission("sys:dict:list")
    public Result<IPage<DictResponse>> page(@Validated DictPageRequest request) {
        return Result.ok(dictService.pageDicts(request));
    }

    @Operation(summary = "根据 ID 查询字典详情")
    @GetMapping("/{id}")
    @RequiresPermission("sys:dict:list")
    public Result<DictResponse> getById(@PathVariable String id) {
        return Result.ok(dictService.getDictById(id));
    }

    @Operation(summary = "新增字典")
    @PostMapping("/create")
    @RequiresPermission("sys:dict:add")
    @OperationLog(module = "字典管理", action = "新增字典")
    public Result<Void> create(@Validated @RequestBody DictCreateRequest request) {
        dictService.createDict(request);
        return Result.ok();
    }

    @Operation(summary = "编辑字典")
    @PostMapping("/update")
    @RequiresPermission("sys:dict:edit")
    @OperationLog(module = "字典管理", action = "编辑字典")
    public Result<Void> update(@Validated @RequestBody DictUpdateRequest request) {
        dictService.updateDict(request);
        return Result.ok();
    }

    @Operation(summary = "删除字典")
    @PostMapping("/delete")
    @RequiresPermission("sys:dict:delete")
    @OperationLog(module = "字典管理", action = "删除字典")
    public Result<Void> delete(@Validated @RequestBody DeleteRequest request) {
        dictService.deleteDict(request.getId());
        return Result.ok();
    }

    @Operation(summary = "批量删除字典")
    @PostMapping("/batch/delete")
    @RequiresPermission("sys:dict:delete")
    @OperationLog(module = "字典管理", action = "批量删除字典")
    public Result<Void> batchDelete(@RequestBody List<String> ids) {
        dictService.batchDeleteDicts(ids);
        return Result.ok();
    }
}
