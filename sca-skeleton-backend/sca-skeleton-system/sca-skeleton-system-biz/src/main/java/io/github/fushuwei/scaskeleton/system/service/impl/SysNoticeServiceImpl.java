package io.github.fushuwei.scaskeleton.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import io.github.fushuwei.scaskeleton.system.api.request.notice.NoticeCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.notice.NoticePageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.notice.NoticeStatusRequest;
import io.github.fushuwei.scaskeleton.system.api.request.notice.NoticeTopRequest;
import io.github.fushuwei.scaskeleton.system.api.request.notice.NoticeUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.notice.NoticeInboxResponse;
import io.github.fushuwei.scaskeleton.system.api.response.notice.NoticeResponse;
import io.github.fushuwei.scaskeleton.system.api.response.notice.NoticeTargetResponse;
import io.github.fushuwei.scaskeleton.system.converter.NoticeConverter;
import io.github.fushuwei.scaskeleton.system.entity.SysNotice;
import io.github.fushuwei.scaskeleton.system.entity.SysNoticeRead;
import io.github.fushuwei.scaskeleton.system.entity.SysNoticeTarget;
import io.github.fushuwei.scaskeleton.system.entity.SysUserDept;
import io.github.fushuwei.scaskeleton.system.entity.SysUserRole;
import io.github.fushuwei.scaskeleton.system.mapper.SysNoticeMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysNoticeReadMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysNoticeTargetMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysUserDeptMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysUserRoleMapper;
import io.github.fushuwei.scaskeleton.system.service.SysNoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 通知公告管理 Service 实现类
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysNoticeServiceImpl implements SysNoticeService {

    private final SysNoticeMapper noticeMapper;
    private final SysNoticeTargetMapper noticeTargetMapper;
    private final SysNoticeReadMapper noticeReadMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserDeptMapper userDeptMapper;
    private final NoticeConverter noticeConverter;

    /**
     * 分页查询通知公告列表
     *
     * @param request 查询条件
     * @return 分页结果
     */
    @Override
    public IPage<NoticeResponse> pageNotices(NoticePageRequest request) {
        Page<NoticeResponse> page = new Page<>(request.getPageNum(), request.getPageSize());
        String tenantId = SecurityUtils.getTenantId();
        return noticeMapper.selectNoticePage(page, tenantId, request);
    }

    /**
     * 根据 ID 查询通知公告详情（包含接收目标列表）
     *
     * @param id 通知公告 ID
     * @return 通知公告详情
     */
    @Override
    public NoticeResponse getNoticeById(String id) {
        String tenantId = SecurityUtils.getTenantId();
        // 查询通知公告基本信息
        NoticeResponse response = noticeMapper.selectNoticeById(id, tenantId);
        if (response == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "通知公告不存在");
        }
        // 查询接收目标列表
        List<SysNoticeTarget> targets = noticeTargetMapper.selectByNoticeId(id);
        List<NoticeTargetResponse> targetResponses = noticeConverter.toNoticeTargetResponseList(targets);
        response.setTargets(targetResponses);
        return response;
    }

    /**
     * 新增通知公告
     *
     * @param request 通知公告信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createNotice(NoticeCreateRequest request) {
        String tenantId = SecurityUtils.getTenantId();
        String userId = SecurityUtils.getUserId();

        // 校验接收目标：当 targetType 不为 all 时，targets 不能为空
        validateTargets(request.getTargetType(), request.getTargets());

        // 封装通知公告实体
        SysNotice notice = new SysNotice();
        notice.setTenantId(tenantId);
        notice.setTitle(request.getTitle());
        notice.setType(request.getType());
        notice.setContent(request.getContent());
        notice.setLevel(request.getLevel());
        notice.setStatus(request.getStatus());
        notice.setIsTop(request.getIsTop() != null ? request.getIsTop() : 0);
        notice.setTopExpireTime(request.getTopExpireTime());
        notice.setIsPopup(request.getIsPopup() != null ? request.getIsPopup() : 0);
        notice.setTargetType(request.getTargetType());
        notice.setReadCount(0);
        notice.setSort(request.getIsTop() != null && request.getIsTop() == 1 ? request.getSort() : null);
        notice.setRemark(request.getRemark());
        notice.setEffectiveTime(request.getEffectiveTime());
        notice.setExpireTime(request.getExpireTime());

        // 如果状态为已发布，设置发布人和发布时间
        if ("published".equals(request.getStatus())) {
            notice.setPublisher(userId);
            notice.setPublishTime(LocalDateTime.now());
            notice.setCreateTime(notice.getPublishTime());
        }

        // 保存通知公告
        noticeMapper.insert(notice);

        // 保存接收目标
        saveTargets(notice.getId(), tenantId, request.getTargetType(), request.getTargets());
    }

    /**
     * 编辑通知公告
     *
     * @param request 通知公告信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNotice(NoticeUpdateRequest request) {
        // 加载通知公告实体
        SysNotice notice = loadNoticeEntity(request.getId());

        // 校验接收目标
        validateTargets(request.getTargetType(), request.getTargets());

        // 已归档的通知公告不允许编辑
        if ("archived".equals(notice.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "已归档的通知公告不允许编辑");
        }

        // 更新字段
        notice.setTitle(request.getTitle());
        notice.setType(request.getType());
        notice.setContent(request.getContent());
        notice.setLevel(request.getLevel());
        notice.setIsTop(request.getIsTop() != null ? request.getIsTop() : 0);
        notice.setTopExpireTime(request.getTopExpireTime());
        notice.setIsPopup(request.getIsPopup() != null ? request.getIsPopup() : 0);
        notice.setTargetType(request.getTargetType());
        notice.setSort(request.getIsTop() != null && request.getIsTop() == 1 ? request.getSort() : null);
        notice.setRemark(request.getRemark());
        notice.setEffectiveTime(request.getEffectiveTime());
        notice.setExpireTime(request.getExpireTime());

        // 乐观锁：使用前端回传的 version 作为 WHERE 条件
        notice.setVersion(request.getVersion());
        int affectedRows = noticeMapper.updateById(notice);
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.VERSION_CONFLICT);
        }

        // 更新接收目标：先删除旧目标，再插入新目标
        noticeTargetMapper.deleteByNoticeId(notice.getId());
        saveTargets(notice.getId(), notice.getTenantId(), request.getTargetType(), request.getTargets());
    }

    /**
     * 变更通知公告状态（发布、撤回、归档）
     *
     * @param request 通知公告 ID 与目标状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNoticeStatus(NoticeStatusRequest request) {
        SysNotice notice = loadNoticeEntity(request.getId());
        String userId = SecurityUtils.getUserId();

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<SysNotice> wrapper = new LambdaUpdateWrapper<SysNotice>()
            .eq(SysNotice::getId, request.getId())
            .set(SysNotice::getStatus, request.getStatus())
            .set(SysNotice::getUpdateBy, userId)
            .set(SysNotice::getUpdateTime, now);

        // 如果是发布操作，设置发布人和发布时间
        if ("published".equals(request.getStatus())) {
            wrapper.set(SysNotice::getPublisher, userId);
            wrapper.set(SysNotice::getPublishTime, now);
        }

        // 撤回或归档时取消置顶：清空 isTop、sort、topExpireTime
        if ("archived".equals(request.getStatus()) || "revoked".equals(request.getStatus())) {
            wrapper.set(SysNotice::getIsTop, 0);
            wrapper.set(SysNotice::getSort, null);
            wrapper.set(SysNotice::getTopExpireTime, null);
        }

        int affectedRows = noticeMapper.update(null, wrapper);
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "通知公告不存在或已被删除");
        }

        /*
         * 撤回时物理删除全部已读记录并清零阅读数：
         * 保证下次重新发布（无论是否编辑内容）时所有人对该公告均为未读状态，
         * 避免用户因历史已读而忽略更新后的重要信息。
         * 注意必须物理删除：逻辑删除会在表中残留旧行，触发唯一键
         * uk_notice_user(notice_id, user_id) 冲突，导致重新发布后无法再次标记已读。
         */
        if ("revoked".equals(request.getStatus())) {
            noticeReadMapper.physicalDeleteByNoticeId(request.getId());
            LambdaUpdateWrapper<SysNotice> resetWrapper = new LambdaUpdateWrapper<SysNotice>()
                .eq(SysNotice::getId, request.getId())
                .set(SysNotice::getReadCount, 0);
            noticeMapper.update(null, resetWrapper);
        }
    }

    /**
     * 置顶/取消置顶通知公告
     *
     * @param request 通知公告 ID 与是否置顶
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNoticeTop(NoticeTopRequest request) {
        String userId = SecurityUtils.getUserId();

        LambdaUpdateWrapper<SysNotice> wrapper = new LambdaUpdateWrapper<SysNotice>()
            .eq(SysNotice::getId, request.getId())
            .set(SysNotice::getIsTop, request.getIsTop())
            .set(SysNotice::getUpdateBy, userId)
            .set(SysNotice::getUpdateTime, LocalDateTime.now());

        // 取消置顶时，清空排序和置顶到期时间
        if (request.getIsTop() == 0) {
            wrapper.set(SysNotice::getSort, null);
            wrapper.set(SysNotice::getTopExpireTime, null);
        }

        int affectedRows = noticeMapper.update(null, wrapper);
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "通知公告不存在或已被删除");
        }
    }

    /**
     * 删除通知公告
     *
     * @param id 通知公告 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotice(String id) {
        loadNoticeEntity(id);
        // 删除通知公告
        noticeMapper.deleteById(id);
        // 删除关联的接收目标
        noticeTargetMapper.deleteByNoticeId(id);
    }

    /**
     * 批量删除通知公告
     *
     * @param ids 通知公告 ID 列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteNotices(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        List<String> distinctIds = ids.stream().distinct().toList();
        // 批量加载并校验
        List<SysNotice> entities = loadNoticeEntities(distinctIds);
        for (SysNotice entity : entities) {
            if (!Objects.equals(entity.getTenantId(), SecurityUtils.getTenantId())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "权限不足，无法操作其他租户的数据");
            }
        }
        // 批量删除通知公告
        noticeMapper.deleteBatchIds(distinctIds);
        // 删除关联的接收目标
        for (String id : distinctIds) {
            noticeTargetMapper.deleteByNoticeId(id);
        }
    }

    /**
     * 标记通知公告为已读
     *
     * @param noticeId 通知公告 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(String noticeId) {
        String tenantId = SecurityUtils.getTenantId();
        String userId = SecurityUtils.getUserId();

        // 校验通知公告存在且对当前用户可见（已发布、有效期内、接收范围包含当前用户），
        // 防止对草稿/已撤回/非本人接收范围的公告标记已读（越权写）
        List<String> deptIds = getUserDeptIds(tenantId, userId);
        List<String> roleIds = getUserRoleIds(tenantId, userId);
        if (noticeMapper.countVisibleNoticeById(tenantId, userId, deptIds, roleIds, noticeId) == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "通知公告不存在或不可见");
        }

        // 检查是否已读
        SysNoticeRead existingRead = noticeReadMapper.selectByNoticeIdAndUserId(noticeId, userId);
        if (existingRead != null) {
            // 已存在已读记录，直接返回（幂等操作）
            return;
        }

        // 新增已读记录（(notice_id, user_id) 唯一键兜底并发重复插入）
        SysNoticeRead readRecord = new SysNoticeRead();
        readRecord.setTenantId(tenantId);
        readRecord.setNoticeId(noticeId);
        readRecord.setUserId(userId);
        readRecord.setReadTime(LocalDateTime.now());
        try {
            noticeReadMapper.insert(readRecord);
        } catch (DuplicateKeyException e) {
            // 并发场景下已被标记已读，直接返回
            return;
        }

        // 更新通知公告的已读次数（+1）
        LambdaUpdateWrapper<SysNotice> wrapper = new LambdaUpdateWrapper<SysNotice>()
            .eq(SysNotice::getId, noticeId)
            .setSql("read_count = read_count + 1");
        noticeMapper.update(null, wrapper);
    }

    /**
     * 查询当前用户的未读通知公告数量
     *
     * @return 未读数量
     */
    @Override
    public long getUnreadCount() {
        String tenantId = SecurityUtils.getTenantId();
        String userId = SecurityUtils.getUserId();

        // 查询当前用户的部门 ID 列表和角色 ID 列表
        List<String> deptIds = getUserDeptIds(tenantId, userId);
        List<String> roleIds = getUserRoleIds(tenantId, userId);

        return noticeMapper.selectUnreadCount(tenantId, userId, deptIds, roleIds);
    }

    /**
     * 分页查询当前用户的消息收件箱（可见通知公告列表，含已读状态）
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @Override
    public IPage<NoticeInboxResponse> getNoticeInbox(int pageNum, int pageSize) {
        String tenantId = SecurityUtils.getTenantId();
        String userId = SecurityUtils.getUserId();

        // 查询当前用户的部门 ID 列表和角色 ID 列表
        List<String> deptIds = getUserDeptIds(tenantId, userId);
        List<String> roleIds = getUserRoleIds(tenantId, userId);

        Page<NoticeInboxResponse> page = new Page<>(pageNum, pageSize);
        return noticeMapper.selectInboxPage(page, tenantId, userId, deptIds, roleIds);
    }

    /**
     * 将当前用户所有未读通知公告标记为已读
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead() {
        String tenantId = SecurityUtils.getTenantId();
        String userId = SecurityUtils.getUserId();

        // 查询当前用户的部门 ID 列表和角色 ID 列表
        List<String> deptIds = getUserDeptIds(tenantId, userId);
        List<String> roleIds = getUserRoleIds(tenantId, userId);

        // 查询所有未读通知公告 ID 列表
        List<String> unreadNoticeIds = noticeMapper.selectUnreadNoticeIds(tenantId, userId, deptIds, roleIds);
        if (CollectionUtils.isEmpty(unreadNoticeIds)) {
            return;
        }

        // 批量插入已读记录
        noticeReadMapper.batchInsertReadRecords(tenantId, userId, unreadNoticeIds);

        // 批量更新通知公告的已读次数（每条 +1，单条 SQL 避免逐条更新）
        noticeMapper.batchIncreaseReadCount(tenantId, unreadNoticeIds);
    }

    // ==================== 私有方法 ====================

    /**
     * 查询当前用户的部门 ID 列表
     * <p>
     * 当用户无部门时返回包含占位符 "none" 的列表，避免 SQL 中 IN () 语法错误。
     *
     * @param tenantId 租户 ID
     * @param userId   用户 ID
     * @return 部门 ID 列表（不为空）
     */
    private List<String> getUserDeptIds(String tenantId, String userId) {
        List<SysUserDept> userDepts = userDeptMapper.selectList(
            new LambdaQueryWrapper<SysUserDept>()
                .eq(SysUserDept::getTenantId, tenantId)
                .eq(SysUserDept::getUserId, userId)
        );
        List<String> deptIds = userDepts.stream()
            .map(SysUserDept::getDeptId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toList());
        return deptIds.isEmpty() ? Collections.singletonList("none") : deptIds;
    }

    /**
     * 查询当前用户的角色 ID 列表
     * <p>
     * 当用户无角色时返回包含占位符 "none" 的列表，避免 SQL 中 IN () 语法错误。
     *
     * @param tenantId 租户 ID
     * @param userId   用户 ID
     * @return 角色 ID 列表（不为空）
     */
    private List<String> getUserRoleIds(String tenantId, String userId) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(
            new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getTenantId, tenantId)
                .eq(SysUserRole::getUserId, userId)
        );
        List<String> roleIds = userRoles.stream()
            .map(SysUserRole::getRoleId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toList());
        return roleIds.isEmpty() ? Collections.singletonList("none") : roleIds;
    }

    /**
     * 根据 ID 加载通知公告实体并校验存在性与租户隔离
     *
     * @param id 通知公告 ID
     * @return 通知公告实体
     */
    private SysNotice loadNoticeEntity(String id) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "通知公告不存在");
        }
        if (!Objects.equals(notice.getTenantId(), SecurityUtils.getTenantId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "权限不足，无法操作其他租户的数据");
        }
        return notice;
    }

    /**
     * 根据 ID 列表批量加载通知公告实体并校验存在性
     *
     * @param ids 通知公告 ID 列表
     * @return 通知公告实体列表
     */
    private List<SysNotice> loadNoticeEntities(List<String> ids) {
        List<SysNotice> entities = noticeMapper.selectBatchIds(ids);
        if (entities.size() != ids.size()) {
            List<String> foundIds = entities.stream().map(SysNotice::getId).toList();
            List<String> missing = ids.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new BusinessException(ResultCode.NOT_FOUND, "通知公告不存在，ID: " + String.join(", ", missing));
        }
        return entities;
    }

    /**
     * 校验接收目标：当 targetType 不为 all 时，targets 不能为空
     *
     * @param targetType 接收范围
     * @param targets    接收目标列表
     */
    private void validateTargets(String targetType, List<NoticeCreateRequest.NoticeTargetItem> targets) {
        if (!"all".equals(targetType)) {
            if (CollectionUtils.isEmpty(targets)) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "接收范围非全体用户时，必须指定接收目标");
            }
        }
    }

    /**
     * 保存接收目标列表
     *
     * @param noticeId   通知公告 ID
     * @param tenantId   租户 ID
     * @param targetType 接收范围
     * @param targets    接收目标列表
     */
    private void saveTargets(String noticeId, String tenantId, String targetType,
                             List<NoticeCreateRequest.NoticeTargetItem> targets) {
        if ("all".equals(targetType) || CollectionUtils.isEmpty(targets)) {
            return;
        }
        List<SysNoticeTarget> targetEntities = new ArrayList<>();
        for (NoticeCreateRequest.NoticeTargetItem item : targets) {
            if (!StringUtils.hasText(item.getTargetId())) {
                continue;
            }
            SysNoticeTarget target = new SysNoticeTarget();
            target.setTenantId(tenantId);
            target.setNoticeId(noticeId);
            target.setTargetType(item.getTargetType());
            target.setTargetId(item.getTargetId());
            targetEntities.add(target);
        }
        if (!targetEntities.isEmpty()) {
            // 批量插入
            for (SysNoticeTarget target : targetEntities) {
                noticeTargetMapper.insert(target);
            }
        }
    }
}
