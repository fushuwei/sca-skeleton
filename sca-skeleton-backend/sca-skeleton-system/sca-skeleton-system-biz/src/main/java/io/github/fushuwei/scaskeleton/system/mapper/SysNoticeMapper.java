package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.notice.NoticePageRequest;
import io.github.fushuwei.scaskeleton.system.api.response.notice.NoticeResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysNotice;
import org.apache.ibatis.annotations.Param;

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
}
