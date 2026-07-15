package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.fushuwei.scaskeleton.system.entity.SysUserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 用户角色关联 Mapper
 *
 * @author Fu Wei
 */
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * 删除用户关联的所有角色
     *
     * @param tenantId 租户 ID
     * @param userId   用户 ID
     * @return 删除行数
     */
    @Delete("DELETE FROM sys_user_role WHERE tenant_id = #{tenantId} AND user_id = #{userId}")
    int physicalDeleteByUser(@Param("tenantId") String tenantId, @Param("userId") String userId);
}
