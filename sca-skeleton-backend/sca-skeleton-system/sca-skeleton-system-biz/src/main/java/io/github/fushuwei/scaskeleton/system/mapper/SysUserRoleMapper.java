package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.fushuwei.scaskeleton.system.entity.SysUserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户角色关联 Mapper
 *
 * @author Fu Wei
 */
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * 删除用户与角色关联关系
     *
     * @param tenantId 租户 ID
     * @param userId   用户 ID
     * @return 删除行数
     */
    @Delete("DELETE FROM sys_user_role WHERE tenant_id = #{tenantId} AND user_id = #{userId}")
    int physicalDeleteByUser(@Param("tenantId") String tenantId, @Param("userId") String userId);

    /**
     * 批量删除用户与角色关联关系
     *
     * @param tenantId 租户 ID
     * @param userIds  用户 ID 列表
     * @return 删除行数
     */
    @Delete("<script>DELETE FROM sys_user_role WHERE tenant_id = #{tenantId} AND user_id IN "
        + "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int physicalDeleteByUsers(@Param("tenantId") String tenantId, @Param("userIds") List<String> userIds);
}
