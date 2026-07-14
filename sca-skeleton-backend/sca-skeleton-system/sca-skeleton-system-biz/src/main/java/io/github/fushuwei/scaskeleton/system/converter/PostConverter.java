package io.github.fushuwei.scaskeleton.system.converter;

import io.github.fushuwei.scaskeleton.system.api.response.post.PostResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysPost;
import org.mapstruct.Mapper;

/**
 * 岗位对象转换器（MapStruct）
 *
 * @author Fu Wei
 */
@Mapper(componentModel = "spring")
public interface PostConverter {

    /** SysPost → PostResponse */
    PostResponse toPostResponse(SysPost post);
}
