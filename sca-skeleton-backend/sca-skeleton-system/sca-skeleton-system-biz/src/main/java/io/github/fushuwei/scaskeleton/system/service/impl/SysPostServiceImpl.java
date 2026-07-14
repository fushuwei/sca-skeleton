package io.github.fushuwei.scaskeleton.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.system.api.request.post.PostCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.post.PostPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.post.PostUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.post.PostResponse;
import io.github.fushuwei.scaskeleton.system.converter.PostConverter;
import io.github.fushuwei.scaskeleton.system.entity.SysPost;
import io.github.fushuwei.scaskeleton.system.mapper.SysPostMapper;
import io.github.fushuwei.scaskeleton.system.service.SysPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 岗位管理服务实现。
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysPostServiceImpl implements SysPostService {

    /** 岗位主表 Mapper */
    private final SysPostMapper postMapper;
    /** Entity ↔ Response 转换器（MapStruct 生成） */
    private final PostConverter postConverter;

    @Override
    public IPage<PostResponse> pagePosts(String tenantId, PostPageRequest req) {
        // 构造分页对象
        Page<SysPost> page = new Page<>(req.getPageNum(), req.getPageSize());

        LambdaQueryWrapper<SysPost> wrapper = new LambdaQueryWrapper<SysPost>()
                // 按租户隔离
                .eq(SysPost::getTenantId, tenantId)
                // 关键词模糊匹配名称或编码
                .and(StringUtils.hasText(req.getKeyword()),
                        w -> w.like(SysPost::getName, req.getKeyword())
                                .or().like(SysPost::getCode, req.getKeyword()));

        // 安全排序：白名单校验通过后按指定字段排序，否则按 sort 升序
        String sortField = req.safeSortField();
        boolean isAsc = "ASC".equalsIgnoreCase(req.safeSortOrder());
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

        // 查询实体分页并转换为响应对象分页
        IPage<SysPost> entityPage = postMapper.selectPage(page, wrapper);
        return entityPage.convert(postConverter::toPostResponse);
    }

    @Override
    public List<PostResponse> listPosts(String tenantId) {
        // 按租户查询全部岗位，按 sort 升序
        List<SysPost> posts = postMapper.selectList(new LambdaQueryWrapper<SysPost>()
                .eq(SysPost::getTenantId, tenantId)
                .orderByAsc(SysPost::getSort));
        // 转换为响应对象列表
        return posts.stream().map(postConverter::toPostResponse).toList();
    }

    @Override
    public PostResponse getPostById(String id) {
        // 按主键查询岗位并转换为响应对象
        return postConverter.toPostResponse(loadPostEntity(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPost(String tenantId, PostCreateRequest req) {
        // 岗位编码在同租户内唯一
        long count = postMapper.selectCount(new LambdaQueryWrapper<SysPost>()
                .eq(SysPost::getTenantId, tenantId)
                .eq(SysPost::getCode, req.getCode()));
        if (count > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "岗位编码已存在");
        }
        // 组装岗位实体
        SysPost post = new SysPost();
        post.setTenantId(tenantId);
        post.setName(req.getName());
        post.setCode(req.getCode());
        post.setSort(req.getSort() != null ? req.getSort() : 100);
        post.setRemark(req.getRemark());
        // 持久化岗位主表
        postMapper.insert(post);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePost(String tenantId, PostUpdateRequest req) {
        // 校验岗位存在并加载当前快照
        SysPost existing = loadPostEntity(req.getId());
        // 编码变更时校验同租户内唯一（排除自身）
        if (StringUtils.hasText(req.getCode()) && !req.getCode().equals(existing.getCode())) {
            long count = postMapper.selectCount(new LambdaQueryWrapper<SysPost>()
                    .eq(SysPost::getTenantId, tenantId)
                    .eq(SysPost::getCode, req.getCode())
                    .ne(SysPost::getId, req.getId()));
            if (count > 0) {
                throw new BusinessException(ResultCode.ALREADY_EXISTS, "岗位编码已存在");
            }
            existing.setCode(req.getCode());
        }
        existing.setName(req.getName());
        existing.setSort(req.getSort() != null ? req.getSort() : existing.getSort());
        existing.setRemark(req.getRemark());
        postMapper.updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(String id) {
        // 校验岗位存在
        loadPostEntity(id);
        // 逻辑删除岗位主表
        postMapper.deleteById(id);
    }

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
     * 按主键加载岗位实体（供内部业务逻辑使用，不对外暴露 Entity）。
     *
     * @param id 岗位 ID
     * @return 岗位实体
     * @throws BusinessException 岗位不存在时抛出 NOT_FOUND
     */
    private SysPost loadPostEntity(String id) {
        SysPost post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "岗位不存在");
        }
        return post;
    }
}
