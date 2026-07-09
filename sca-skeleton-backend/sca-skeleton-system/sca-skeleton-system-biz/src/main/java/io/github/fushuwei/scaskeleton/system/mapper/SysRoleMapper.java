package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
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
     * @param page           分页对象
     * @param queryWrapper   查询条件
     * @param orderByOverride 非表字段排序（如 permission_count ASC），可为 null
     * @return 分页结果（每条记录含 permissionCount）
     */
    IPage<SysRole> selectRolePage(IPage<SysRole> page,
                                  @Param(Constants.WRAPPER) Wrapper<SysRole> queryWrapper,
                                  @Param("orderByOverride") String orderByOverride);

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
