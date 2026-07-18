package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.post.PostPageRequest;
import io.github.fushuwei.scaskeleton.system.api.response.post.PostResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysPost;
import org.apache.ibatis.annotations.Param;

/**
 * 岗位管理 Mapper
 *
 * @author Fu Wei
 */
public interface SysPostMapper extends BaseMapper<SysPost> {

    /**
     * 分页查询岗位列表
     *
     * @param page     分页对象
     * @param tenantId 租户 ID
     * @param request  查询条件
     * @return 分页结果
     */
    IPage<PostResponse> selectPostPage(IPage<PostResponse> page, @Param("tenantId") String tenantId, @Param("request") PostPageRequest request);
}
