package io.github.fushuwei.scaskeleton.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.notice.NoticeCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.notice.NoticePageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.notice.NoticeStatusRequest;
import io.github.fushuwei.scaskeleton.system.api.request.notice.NoticeTopRequest;
import io.github.fushuwei.scaskeleton.system.api.request.notice.NoticeUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.notice.NoticeInboxResponse;
import io.github.fushuwei.scaskeleton.system.api.response.notice.NoticeResponse;

import java.util.List;

/**
 * 通知公告管理 Service
 *
 * @author Fu Wei
 */
public interface SysNoticeService {

    /**
     * 分页查询通知公告列表
     *
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<NoticeResponse> pageNotices(NoticePageRequest request);

    /**
     * 根据 ID 查询通知公告详情（包含接收目标列表）
     *
     * @param id 通知公告 ID
     * @return 通知公告详情
     */
    NoticeResponse getNoticeById(String id);

    /**
     * 新增通知公告
     *
     * @param request 通知公告信息
     */
    void createNotice(NoticeCreateRequest request);

    /**
     * 编辑通知公告
     *
     * @param request 通知公告信息
     */
    void updateNotice(NoticeUpdateRequest request);

    /**
     * 变更通知公告状态（发布、撤回、归档）
     *
     * @param request 通知公告 ID 与目标状态
     */
    void updateNoticeStatus(NoticeStatusRequest request);

    /**
     * 置顶/取消置顶通知公告
     *
     * @param request 通知公告 ID 与是否置顶
     */
    void updateNoticeTop(NoticeTopRequest request);

    /**
     * 删除通知公告
     *
     * @param id 通知公告 ID
     */
    void deleteNotice(String id);

    /**
     * 批量删除通知公告
     *
     * @param ids 通知公告 ID 列表
     */
    void batchDeleteNotices(List<String> ids);

    /**
     * 标记通知公告为已读
     *
     * @param noticeId 通知公告 ID
     */
    void markAsRead(String noticeId);

    /**
     * 查询当前用户的未读通知公告数量
     *
     * @return 未读数量
     */
    long getUnreadCount();

    /**
     * 分页查询当前用户的消息收件箱（可见通知公告列表，含已读状态）
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    IPage<NoticeInboxResponse> getNoticeInbox(int pageNum, int pageSize);

    /**
     * 将当前用户所有未读通知公告标记为已读
     */
    void markAllAsRead();
}
