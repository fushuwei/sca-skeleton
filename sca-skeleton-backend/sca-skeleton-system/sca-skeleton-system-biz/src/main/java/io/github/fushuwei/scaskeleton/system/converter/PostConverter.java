package io.github.fushuwei.scaskeleton.system.converter;

import io.github.fushuwei.scaskeleton.system.api.response.post.PostOptionResponse;
import io.github.fushuwei.scaskeleton.system.api.response.post.PostResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysPost;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 岗位对象转换器（MapStruct）
 *
 * @author Fu Wei
 */
@Mapper(componentModel = "spring")
public interface PostConverter {

    /**
     * 岗位实体 → 岗位响应
     *
     * @param post 岗位实体
     * @return 岗位响应对象
     */
    PostResponse toPostResponse(SysPost post);

    /**
     * 岗位实体 → 岗位选项响应
     *
     * @param post 岗位实体
     * @return 岗位选项响应对象
     */
    PostOptionResponse toPostOptionResponse(SysPost post);

    /**
     * 岗位实体列表 → 岗位选项响应列表
     *
     * @param posts 岗位实体列表
     * @return 岗位选项响应列表
     */
    List<PostOptionResponse> toPostOptionResponseList(List<SysPost> posts);
}
