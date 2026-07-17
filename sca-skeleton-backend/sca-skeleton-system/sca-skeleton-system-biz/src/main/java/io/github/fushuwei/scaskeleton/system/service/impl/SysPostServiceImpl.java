package io.github.fushuwei.scaskeleton.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import io.github.fushuwei.scaskeleton.system.api.request.post.PostCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.post.PostPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.post.PostUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.post.PostResponse;
import io.github.fushuwei.scaskeleton.system.converter.PostConverter;
import io.github.fushuwei.scaskeleton.system.entity.SysPost;
import io.github.fushuwei.scaskeleton.system.entity.SysTenant;
import io.github.fushuwei.scaskeleton.system.mapper.SysPostMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysTenantMapper;
import io.github.fushuwei.scaskeleton.system.service.SysPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 岗位管理 Service 实现类
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysPostServiceImpl implements SysPostService {

    private final SysPostMapper postMapper;

    private final SysTenantMapper tenantMapper;

    private final PostConverter postConverter;

    /**
     * 查询岗位列表
     *
     * @return 岗位列表
     */
    @Override
    public List<PostResponse> listPosts() {
        // 数据隔离：超管看所有租户，非超管只看自己租户
        List<SysPost> posts = postMapper.selectList(new LambdaQueryWrapper<SysPost>()
            .eq(!SecurityUtils.isSuperAdmin(), SysPost::getTenantId, SecurityUtils.getTenantId())
            .orderByAsc(SysPost::getSort));
        // 转换为响应对象列表
        return posts.stream().map(postConverter::toPostResponse).toList();
    }

    /**
     * 分页查询岗位列表
     *
     * @param request 查询条件
     * @return 分页结果
     */
    @Override
    public IPage<PostResponse> pagePosts(PostPageRequest request) {
        // 构造分页对象
        Page<SysPost> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<SysPost> wrapper = new LambdaQueryWrapper<SysPost>()
            // 数据隔离：超管看所有租户，非超管只看自己租户
            .eq(!SecurityUtils.isSuperAdmin(), SysPost::getTenantId, SecurityUtils.getTenantId())
            // 关键词模糊匹配名称或编码
            .and(StringUtils.hasText(request.getKeyword()),
                w -> w.like(SysPost::getName, request.getKeyword())
                    .or().like(SysPost::getCode, request.getKeyword()));

        // 安全排序：白名单校验通过后按指定字段排序，默认按 sort 升序
        String sortField = request.safeSortField();
        boolean isAsc = "ASC".equalsIgnoreCase(request.safeSortOrder());
        if (sortField != null) {
            switch (sortField) {
                case "name" -> wrapper.orderBy(true, isAsc, SysPost::getName);
                case "code" -> wrapper.orderBy(true, isAsc, SysPost::getCode);
                case "sort" -> wrapper.orderBy(true, isAsc, SysPost::getSort);
                case "create_time" -> wrapper.orderBy(true, isAsc, SysPost::getCreateTime);
            }
        } else {
            wrapper.orderByAsc(SysPost::getSort);
        }

        // 查询分页数据，并将结果转换为响应对象
        IPage<SysPost> entityPage = postMapper.selectPage(page, wrapper);
        return entityPage.convert(postConverter::toPostResponse);
    }

    /**
     * 根据 ID 查询岗位详情
     *
     * @param id 岗位 ID
     * @return 岗位详情
     */
    @Override
    public PostResponse getPostById(String id) {
        // 加载岗位实体并转换为响应对象
        return postConverter.toPostResponse(loadPostEntity(id));
    }

    /**
     * 新增岗位
     *
     * @param request 岗位信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPost(PostCreateRequest request) {
        // 获取租户 ID（如果是超管创建，该值由前端页面传入，如果是租户内部用户自己创建，则取当前登录人所在租户的 ID）
        String tenantId = resolveTenantId(request.getTenantId());

        // 岗位编码在同一个租户内唯一
        long count = postMapper.selectCount(new LambdaQueryWrapper<SysPost>()
            .eq(SysPost::getTenantId, tenantId)
            .eq(SysPost::getCode, request.getCode()));
        if (count > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "岗位编码已存在");
        }

        // 封装岗位实体
        SysPost post = new SysPost();
        post.setTenantId(tenantId);
        post.setName(request.getName());
        post.setCode(request.getCode());
        post.setSort(request.getSort() != null ? request.getSort() : 100);
        post.setRemark(request.getRemark());

        // 保存岗位
        postMapper.insert(post);
    }

    /**
     * 编辑岗位
     *
     * @param request 岗位信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePost(PostUpdateRequest request) {
        // 加载岗位实体
        SysPost post = loadPostEntity(request.getId());

        // 岗位编码在同一个租户内唯一（排除自身）
        if (StringUtils.hasText(request.getCode()) && !request.getCode().equals(post.getCode())) {
            long codeCount = postMapper.selectCount(new LambdaQueryWrapper<SysPost>()
                .eq(SysPost::getTenantId, post.getTenantId())
                .eq(SysPost::getCode, request.getCode())
                .ne(SysPost::getId, request.getId()));
            if (codeCount > 0) {
                throw new BusinessException(ResultCode.ALREADY_EXISTS, "岗位编码已存在");
            }
            post.setCode(request.getCode());
        }

        // 更新其他字段
        post.setName(request.getName());
        post.setSort(request.getSort() != null ? request.getSort() : post.getSort());
        post.setRemark(request.getRemark());

        // 更新岗位
        postMapper.updateById(post);
    }

    /**
     * 删除岗位
     *
     * @param id 岗位 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(String id) {
        // 加载岗位实体
        loadPostEntity(id);

        // 删除岗位
        postMapper.deleteById(id);
    }

    /**
     * 批量删除岗位
     *
     * @param ids 岗位 ID 列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeletePosts(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        for (String id : ids) {
            deletePost(id);
        }
    }

    /**
     * 解析创建时的目标租户 ID
     *
     * @param requestTenantId 创建时传入的目标租户 ID（仅当超管创建时才会使用该参数）
     * @return 实际写入用的租户 ID
     */
    private String resolveTenantId(String requestTenantId) {
        if (SecurityUtils.isSuperAdmin()) {
            if (!StringUtils.hasText(requestTenantId)) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "超管创建需指定目标租户");
            }
            SysTenant tenant = tenantMapper.selectById(requestTenantId);
            if (tenant == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "目标租户不存在");
            }
            return requestTenantId;
        }
        return SecurityUtils.getTenantId();
    }

    /**
     * 根据 ID 加载岗位实体
     *
     * @param id 岗位 ID
     * @return 岗位实体
     */
    private SysPost loadPostEntity(String id) {
        SysPost post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "岗位不存在");
        }
        if (!SecurityUtils.isSuperAdmin()
            && !Objects.equals(post.getTenantId(), SecurityUtils.getTenantId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "权限不足，无法操作其他租户的数据");
        }
        return post;
    }
}
