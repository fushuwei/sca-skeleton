package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.fushuwei.scaskeleton.system.entity.SysNoticeRead;
import org.apache.ibatis.annotations.Param;

/**
 * 通知公告已读记录 Mapper
 *
 * @author Fu Wei
 */
public interface SysNoticeReadMapper extends BaseMapper<SysNoticeRead> {

    /**
     * 根据通知公告 ID 和用户 ID 查询已读记录
     *
     * @param noticeId 通知公告 ID
     * @param userId   用户 ID
     * @return 已读记录（不存在则返回 null）
     */
    SysNoticeRead selectByNoticeIdAndUserId(@Param("noticeId") String noticeId, @Param("userId") String userId);
}
