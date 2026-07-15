package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.fushuwei.scaskeleton.system.entity.SysUserPost;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 用户岗位关联 Mapper
 *
 * @author Fu Wei
 */
public interface SysUserPostMapper extends BaseMapper<SysUserPost> {

    /**
     * 删除用户关联的所有岗位
     *
     * @param tenantId 租户 ID
     * @param userId   用户 ID
     * @return 删除行数
     */
    @Delete("DELETE FROM sys_user_post WHERE tenant_id = #{tenantId} AND user_id = #{userId}")
    int physicalDeleteByUser(@Param("tenantId") String tenantId, @Param("userId") String userId);
}
