package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.fushuwei.scaskeleton.system.entity.SysUserDept;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 用户部门关联 Mapper。
 *
 * @author Fu Wei
 */
public interface SysUserDeptMapper extends BaseMapper<SysUserDept> {

    /**
     * 物理删除用户的所有部门关联（绕过逻辑删除，避免唯一键冲突）。
     *
     * @param tenantId 租户ID
     * @param userId   用户ID
     * @return 删除行数
     */
    @Delete("DELETE FROM sys_user_dept WHERE tenant_id = #{tenantId} AND user_id = #{userId}")
    int physicalDeleteByUser(@Param("tenantId") String tenantId, @Param("userId") String userId);
}
