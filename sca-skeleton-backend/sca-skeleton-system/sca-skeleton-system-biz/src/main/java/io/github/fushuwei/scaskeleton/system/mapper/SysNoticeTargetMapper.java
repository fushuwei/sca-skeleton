package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.fushuwei.scaskeleton.system.entity.SysNoticeTarget;

import java.util.List;

/**
 * 通知公告接收目标 Mapper
 *
 * @author Fu Wei
 */
public interface SysNoticeTargetMapper extends BaseMapper<SysNoticeTarget> {

    /**
     * 根据通知公告 ID 查询接收目标列表
     *
     * @param noticeId 通知公告 ID
     * @return 接收目标列表
     */
    List<SysNoticeTarget> selectByNoticeId(String noticeId);

    /**
     * 根据通知公告 ID 删除接收目标（物理删除，避免唯一索引冲突）
     *
     * @param noticeId 通知公告 ID
     * @return 影响行数
     */
    int deleteByNoticeId(String noticeId);
}
