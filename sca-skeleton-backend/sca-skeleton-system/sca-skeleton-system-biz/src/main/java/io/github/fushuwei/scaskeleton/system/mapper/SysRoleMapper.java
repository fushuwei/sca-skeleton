package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.fushuwei.scaskeleton.system.entity.SysRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色 Mapper。
 *
 * @author Fu Wei
 */
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 查询指定用户的角色列表。
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return 角色列表
     */
    List<SysRole> selectRolesByUserId(@Param("userId") String userId,
                                      @Param("tenantId") String tenantId);
}
