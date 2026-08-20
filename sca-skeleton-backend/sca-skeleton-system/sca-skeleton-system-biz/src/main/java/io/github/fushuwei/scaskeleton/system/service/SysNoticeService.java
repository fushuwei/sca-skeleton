package io.github.fushuwei.scaskeleton.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.notice.NoticeCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.notice.NoticePageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.notice.NoticeStatusRequest;
import io.github.fushuwei.scaskeleton.system.api.request.notice.NoticeUpdateRequest;
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
}
