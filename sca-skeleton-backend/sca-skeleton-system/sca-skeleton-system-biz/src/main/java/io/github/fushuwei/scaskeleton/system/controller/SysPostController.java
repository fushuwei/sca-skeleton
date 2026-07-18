package io.github.fushuwei.scaskeleton.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.log.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.github.fushuwei.scaskeleton.system.api.request.DeleteRequest;
import io.github.fushuwei.scaskeleton.system.api.request.post.PostCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.post.PostPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.post.PostUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.post.PostOptionResponse;
import io.github.fushuwei.scaskeleton.system.api.response.post.PostResponse;
import io.github.fushuwei.scaskeleton.system.service.SysPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 岗位管理 Controller
 *
 * @author Fu Wei
 */
@Tag(name = "岗位管理")
@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
public class SysPostController {

    private final SysPostService postService;

    @Operation(summary = "查询岗位列表")
    @GetMapping("/list")
    @RequiresPermission("sys:post:list")
    public Result<List<PostResponse>> list() {
        return Result.ok(postService.listPosts());
    }

    @Operation(summary = "查询岗位选项列表", description = "用于用户管理等功能表单下拉选择")
    @GetMapping("/options")
    @RequiresPermission("sys:user:list")
    public Result<List<PostOptionResponse>> options() {
        return Result.ok(postService.listPostOptions());
    }

    @Operation(summary = "分页查询岗位列表")
    @GetMapping("/page")
    @RequiresPermission("sys:post:list")
    public Result<IPage<PostResponse>> page(@Validated PostPageRequest request) {
        return Result.ok(postService.pagePosts(request));
    }

    @Operation(summary = "根据 ID 查询岗位详情")
    @GetMapping("/{id}")
    @RequiresPermission("sys:post:list")
    public Result<PostResponse> getById(@PathVariable String id) {
        return Result.ok(postService.getPostById(id));
    }

    @Operation(summary = "新增岗位")
    @PostMapping("/create")
    @RequiresPermission("sys:post:add")
    @OperationLog(module = "岗位管理", action = "新增岗位")
    public Result<Void> create(@Validated @RequestBody PostCreateRequest request) {
        postService.createPost(request);
        return Result.ok();
    }

    @Operation(summary = "编辑岗位")
    @PostMapping("/update")
    @RequiresPermission("sys:post:edit")
    @OperationLog(module = "岗位管理", action = "编辑岗位")
    public Result<Void> update(@Validated @RequestBody PostUpdateRequest request) {
        postService.updatePost(request);
        return Result.ok();
    }

    @Operation(summary = "删除岗位")
    @PostMapping("/delete")
    @RequiresPermission("sys:post:delete")
    @OperationLog(module = "岗位管理", action = "删除岗位")
    public Result<Void> delete(@Validated @RequestBody DeleteRequest request) {
        postService.deletePost(request.getId());
        return Result.ok();
    }

    @Operation(summary = "批量删除岗位")
    @PostMapping("/batch/delete")
    @RequiresPermission("sys:post:delete")
    @OperationLog(module = "岗位管理", action = "批量删除岗位")
    public Result<Void> batchDelete(@RequestBody List<String> ids) {
        postService.batchDeletePosts(ids);
        return Result.ok();
    }
}
