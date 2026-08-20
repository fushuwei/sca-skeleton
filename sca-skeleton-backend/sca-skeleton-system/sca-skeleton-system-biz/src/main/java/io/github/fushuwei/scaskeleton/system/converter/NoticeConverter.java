package io.github.fushuwei.scaskeleton.system.converter;

import io.github.fushuwei.scaskeleton.system.api.response.notice.NoticeResponse;
import io.github.fushuwei.scaskeleton.system.api.response.notice.NoticeTargetResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysNotice;
import io.github.fushuwei.scaskeleton.system.entity.SysNoticeTarget;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 通知公告对象转换器（MapStruct）
 *
 * @author Fu Wei
 */
@Mapper(componentModel = "spring")
public interface NoticeConverter {

    /**
     * 通知公告实体 → 通知公告响应
     *
     * @param notice 通知公告实体
     * @return 通知公告响应对象
     */
    NoticeResponse toNoticeResponse(SysNotice notice);

    /**
     * 通知公告实体列表 → 通知公告响应列表
     *
     * @param notices 通知公告实体列表
     * @return 通知公告响应列表
     */
    List<NoticeResponse> toNoticeResponseList(List<SysNotice> notices);

    /**
     * 接收目标实体 → 接收目标响应
     *
     * @param target 接收目标实体
     * @return 接收目标响应对象
     */
    NoticeTargetResponse toNoticeTargetResponse(SysNoticeTarget target);

    /**
     * 接收目标实体列表 → 接收目标响应列表
     *
     * @param targets 接收目标实体列表
     * @return 接收目标响应列表
     */
    List<NoticeTargetResponse> toNoticeTargetResponseList(List<SysNoticeTarget> targets);
}
