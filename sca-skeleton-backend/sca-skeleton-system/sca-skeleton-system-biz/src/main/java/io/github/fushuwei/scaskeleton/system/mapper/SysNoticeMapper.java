package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.notice.NoticePageRequest;
import io.github.fushuwei.scaskeleton.system.api.response.notice.NoticeInboxResponse;
import io.github.fushuwei.scaskeleton.system.api.response.notice.NoticeResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysNotice;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知公告管理 Mapper
 *
 * @author Fu Wei
 */
public interface SysNoticeMapper extends BaseMapper<SysNotice> {

    /**
     * 分页查询通知公告列表
     *
     * @param page     分页对象
     * @param tenantId 租户 ID
     * @param request  查询条件
     * @return 分页结果
     */
    IPage<NoticeResponse> selectNoticePage(IPage<NoticeResponse> page, @Param("tenantId") String tenantId, @Param("request") NoticePageRequest request);

    /**
     * 根据 ID 查询通知公告详情（包含接收目标列表）
     *
     * @param id       通知公告 ID
     * @param tenantId 租户 ID
     * @return 通知公告详情
     */
    NoticeResponse selectNoticeById(@Param("id") String id, @Param("tenantId") String tenantId);

    /**
     * 分页查询当前用户可见的通知公告（收件箱），并关联已读状态
     * <p>
     * 可见条件：状态为已发布（published），且在有效期内，且接收范围匹配当前用户
     * （all 全体用户，或通过 sys_notice_target 匹配用户的部门/角色/用户 ID）。
     * 通过 LEFT JOIN sys_notice_read 判断当前用户是否已读。
     *
     * @param page     分页对象
     * @param tenantId 租户 ID
     * @param userId   当前用户 ID
     * @param deptIds  当前用户所属部门 ID 列表
     * @param roleIds  当前用户所属角色 ID 列表
     * @return 分页结果，每条记录包含已读状态
     */
    IPage<NoticeInboxResponse> selectInboxPage(IPage<NoticeInboxResponse> page,
                                                @Param("tenantId") String tenantId,
                                                @Param("userId") String userId,
                                                @Param("deptIds") List<String> deptIds,
                                                @Param("roleIds") List<String> roleIds);

    /**
     * 查询当前用户未读通知公告数量
     * <p>
     * 未读条件：用户可见但 sys_notice_read 中无对应已读记录。
     *
     * @param tenantId 租户 ID
     * @param userId   当前用户 ID
     * @param deptIds  当前用户所属部门 ID 列表
     * @param roleIds  当前用户所属角色 ID 列表
     * @return 未读数量
     */
    long selectUnreadCount(@Param("tenantId") String tenantId,
                           @Param("userId") String userId,
                           @Param("deptIds") List<String> deptIds,
                           @Param("roleIds") List<String> roleIds);

    /**
     * 查询当前用户所有未读通知公告 ID 列表（用于「全部标记已读」）
     *
     * @param tenantId 租户 ID
     * @param userId   当前用户 ID
     * @param deptIds  当前用户所属部门 ID 列表
     * @param roleIds  当前用户所属角色 ID 列表
     * @return 未读通知公告 ID 列表
     */
    List<String> selectUnreadNoticeIds(@Param("tenantId") String tenantId,
                                       @Param("userId") String userId,
                                       @Param("deptIds") List<String> deptIds,
                                       @Param("roleIds") List<String> roleIds);

    /**
     * 校验指定通知公告对当前用户是否可见（用于「标记已读」前的越权校验）
     * <p>
     * 可见条件与收件箱一致：已发布、有效期内、接收范围匹配当前用户。
     *
     * @param tenantId 租户 ID
     * @param userId   当前用户 ID
     * @param deptIds  当前用户所属部门 ID 列表
     * @param roleIds  当前用户所属角色 ID 列表
     * @param id       通知公告 ID
     * @return 可见返回 1，不可见或不存在返回 0
     */
    int countVisibleNoticeById(@Param("tenantId") String tenantId,
                               @Param("userId") String userId,
                               @Param("deptIds") List<String> deptIds,
                               @Param("roleIds") List<String> roleIds,
                               @Param("id") String id);

    /**
     * 批量更新通知公告已读次数（每条 +1，用于「全部标记已读」）
     *
     * @param tenantId 租户 ID
     * @param ids      通知公告 ID 列表
     * @return 受影响行数
     */
    int batchIncreaseReadCount(@Param("tenantId") String tenantId,
                               @Param("ids") List<String> ids);
}
