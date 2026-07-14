package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.role.RolePageRequest;
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
     * 分页查询角色，关联子查询一次性查出权限数量。
     *
     * @param page     分页对象
     * @param tenantId 租户ID（来自 SecurityContext，非 Request）
     * @param req      分页查询请求对象
     * @return 分页结果（每条记录含 permissionCount）
     */
    IPage<SysRole> selectRolePage(IPage<SysRole> page,
                                  @Param("tenantId") String tenantId,
                                  @Param("req") RolePageRequest req);

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
