package io.github.fushuwei.scaskeleton.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.post.PostCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.post.PostPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.post.PostUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.post.PostResponse;

import java.util.List;

/**
 * 岗位管理服务接口。
 *
 * @author Fu Wei
 */
public interface SysPostService {

    /** 分页查询岗位（按租户隔离） */
    IPage<PostResponse> pagePosts(String tenantId, PostPageRequest request);

    List<PostResponse> listPosts(String tenantId);

    PostResponse getPostById(String id);

    void createPost(String tenantId, PostCreateRequest request);

    void updatePost(String tenantId, PostUpdateRequest request);

    void deletePost(String id);

    /** 批量删除岗位 */
    void batchDeletePosts(List<String> ids);
}
