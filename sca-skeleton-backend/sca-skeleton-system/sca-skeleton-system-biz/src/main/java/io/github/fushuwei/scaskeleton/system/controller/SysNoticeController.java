package io.github.fushuwei.scaskeleton.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.log.annotation.OperationLog;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import io.github.fushuwei.scaskeleton.system.api.request.DeleteRequest;
import io.github.fushuwei.scaskeleton.system.api.request.notice.NoticeCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.notice.NoticePageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.notice.NoticeReadRequest;
import io.github.fushuwei.scaskeleton.system.api.request.notice.NoticeStatusRequest;
import io.github.fushuwei.scaskeleton.system.api.request.notice.NoticeTopRequest;
import io.github.fushuwei.scaskeleton.system.api.request.notice.NoticeUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.notice.NoticeInboxResponse;
import io.github.fushuwei.scaskeleton.system.api.response.notice.NoticeResponse;
import io.github.fushuwei.scaskeleton.system.service.SysNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知公告管理 Controller
 *
 * @author Fu Wei
 */
@Tag(name = "通知公告管理")
@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
public class SysNoticeController {

    private final SysNoticeService noticeService;

    @Operation(summary = "分页查询通知公告列表")
    @GetMapping("/page")
    @RequiresPermission("sys:notice:list")
    public Result<IPage<NoticeResponse>> page(@Validated NoticePageRequest request) {
        return Result.ok(noticeService.pageNotices(request));
    }

    @Operation(summary = "根据 ID 查询通知公告详情")
    @GetMapping("/{id}")
    @RequiresPermission("sys:notice:list")
    public Result<NoticeResponse> getById(@PathVariable String id) {
        return Result.ok(noticeService.getNoticeById(id));
    }

    @Operation(summary = "新增通知公告")
    @PostMapping("/create")
    @RequiresPermission("sys:notice:add")
    @OperationLog(module = "通知公告管理", action = "新增通知公告")
    public Result<Void> create(@Validated @RequestBody NoticeCreateRequest request) {
        noticeService.createNotice(request);
        return Result.ok();
    }

    @Operation(summary = "编辑通知公告")
    @PostMapping("/update")
    @RequiresPermission("sys:notice:edit")
    @OperationLog(module = "通知公告管理", action = "编辑通知公告")
    public Result<Void> update(@Validated @RequestBody NoticeUpdateRequest request) {
        noticeService.updateNotice(request);
        return Result.ok();
    }

    @Operation(summary = "变更通知公告状态（发布、撤回、归档）")
    @PostMapping("/status")
    @RequiresPermission("sys:notice:edit")
    @OperationLog(module = "通知公告管理", action = "变更通知公告状态")
    public Result<Void> updateStatus(@Validated @RequestBody NoticeStatusRequest request) {
        noticeService.updateNoticeStatus(request);
        return Result.ok();
    }

    @Operation(summary = "置顶/取消置顶通知公告")
    @PostMapping("/top")
    @RequiresPermission("sys:notice:edit")
    @OperationLog(module = "通知公告管理", action = "置顶/取消置顶通知公告")
    public Result<Void> updateTop(@Validated @RequestBody NoticeTopRequest request) {
        noticeService.updateNoticeTop(request);
        return Result.ok();
    }

    @Operation(summary = "删除通知公告")
    @PostMapping("/delete")
    @RequiresPermission("sys:notice:delete")
    @OperationLog(module = "通知公告管理", action = "删除通知公告")
    public Result<Void> delete(@Validated @RequestBody DeleteRequest request) {
        noticeService.deleteNotice(request.getId());
        return Result.ok();
    }

    @Operation(summary = "批量删除通知公告")
    @PostMapping("/batch/delete")
    @RequiresPermission("sys:notice:delete")
    @OperationLog(module = "通知公告管理", action = "批量删除通知公告")
    public Result<Void> batchDelete(@RequestBody List<String> ids) {
        noticeService.batchDeleteNotices(ids);
        return Result.ok();
    }

    @Operation(summary = "标记通知公告为已读")
    @PostMapping("/read")
    public Result<Void> markAsRead(@Validated @RequestBody NoticeReadRequest request) {
        noticeService.markAsRead(request.getId());
        return Result.ok();
    }

    @Operation(summary = "查询当前用户未读通知数量")
    @GetMapping("/unread/count")
    public Result<Long> getUnreadCount() {
        return Result.ok(noticeService.getUnreadCount());
    }

    @Operation(summary = "查询当前用户消息收件箱（弹窗展示用）")
    @GetMapping("/inbox")
    public Result<IPage<NoticeInboxResponse>> getInbox(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(noticeService.getNoticeInbox(pageNum, pageSize));
    }

    @Operation(summary = "全部标记已读")
    @PostMapping("/read/all")
    public Result<Void> markAllAsRead() {
        noticeService.markAllAsRead();
        return Result.ok();
    }
}
