package io.github.fushuwei.scaskeleton.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.log.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import io.github.fushuwei.scaskeleton.system.api.request.post.PostCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.post.PostPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.post.PostUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.post.PostResponse;
import io.github.fushuwei.scaskeleton.system.service.SysPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 岗位管理 Controller。
 *
 * @author Fu Wei
 */
@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
public class SysPostController {

    private final SysPostService postService;

    // 分页查询当前租户下岗位列表，需 sys:post:list
    @GetMapping("/page")
    @RequiresPermission("sys:post:list")
    public Result<IPage<PostResponse>> page(@Validated PostPageRequest request) {
        return Result.ok(postService.pagePosts(SecurityUtils.getTenantId(), request));
    }

    // 查询当前租户下岗位列表，需 sys:post:list
    @GetMapping("/list")
    @RequiresPermission("sys:post:list")
    public Result<List<PostResponse>> list() {
        return Result.ok(postService.listPosts(SecurityUtils.getTenantId()));
    }

    // 按 ID 查询岗位详情，需 sys:post:query
    @GetMapping("/{id}")
    @RequiresPermission("sys:post:query")
    public Result<PostResponse> getById(@PathVariable("id") String id) {
        return Result.ok(postService.getPostById(id));
    }

    // 在当前租户下创建岗位，需 sys:post:add
    @PostMapping
    @RequiresPermission("sys:post:add")
    @OperationLog(module = "岗位管理", action = "添加岗位")
    public Result<Void> create(
            @Validated @RequestBody PostCreateRequest request) {
        postService.createPost(SecurityUtils.getTenantId(), request);
        return Result.ok();
    }

    // 更新当前租户下岗位信息，需 sys:post:edit
    @PutMapping
    @RequiresPermission("sys:post:edit")
    @OperationLog(module = "岗位管理", action = "编辑岗位")
    public Result<Void> update(
            @Validated @RequestBody PostUpdateRequest request) {
        postService.updatePost(SecurityUtils.getTenantId(), request);
        return Result.ok();
    }

    // 删除指定岗位，需 sys:post:delete
    @DeleteMapping("/{id}")
    @RequiresPermission("sys:post:delete")
    @OperationLog(module = "岗位管理", action = "删除岗位")
    public Result<Void> delete(@PathVariable("id") String id) {
        postService.deletePost(id);
        return Result.ok();
    }

    // 批量删除岗位
    @DeleteMapping("/batch")
    @RequiresPermission("sys:post:delete")
    @OperationLog(module = "岗位管理", action = "批量删除岗位")
    public Result<Void> batchDelete(@RequestBody List<String> ids) {
        postService.batchDeletePosts(ids);
        return Result.ok();
    }
}
