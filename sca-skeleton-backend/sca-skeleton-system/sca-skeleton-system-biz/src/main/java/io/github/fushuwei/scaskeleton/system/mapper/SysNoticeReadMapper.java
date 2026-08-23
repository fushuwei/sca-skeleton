package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.fushuwei.scaskeleton.system.entity.SysNoticeRead;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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

    /**
     * 查询用户所有已读通知公告 ID 列表
     *
     * @param userId 用户 ID
     * @return 已读通知公告 ID 列表
     */
    List<String> selectReadNoticeIdsByUserId(@Param("userId") String userId);

    /**
     * 批量插入已读记录（用于「全部标记已读」）
     *
     * @param tenantId  租户 ID
     * @param userId    用户 ID
     * @param noticeIds 通知公告 ID 列表
     * @return 插入行数
     */
    int batchInsertReadRecords(@Param("tenantId") String tenantId, @Param("userId") String userId, @Param("noticeIds") List<String> noticeIds);
}
