package io.github.fushuwei.scaskeleton.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.post.PostCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.post.PostPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.post.PostUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.post.PostResponse;

import java.util.List;

/**
 * 岗位管理 Service
 *
 * @author Fu Wei
 */
public interface SysPostService {

    /**
     * 查询岗位列表
     *
     * @return 岗位列表
     */
    List<PostResponse> listPosts();

    /**
     * 分页查询岗位列表
     *
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<PostResponse> pagePosts(PostPageRequest request);

    /**
     * 根据 ID 查询岗位详情
     *
     * @param id 岗位 ID
     * @return 岗位详情
     */
    PostResponse getPostById(String id);

    /**
     * 新增岗位
     *
     * @param request 岗位信息
     */
    void createPost(PostCreateRequest request);

    /**
     * 编辑岗位
     *
     * @param request 岗位信息
     */
    void updatePost(PostUpdateRequest request);

    /**
     * 删除岗位
     *
     * @param id 岗位 ID
     */
    void deletePost(String id);

    /**
     * 批量删除岗位
     *
     * @param ids 岗位 ID 列表
     */
    void batchDeletePosts(List<String> ids);
}
