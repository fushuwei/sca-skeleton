package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.role.RolePageRequest;
import io.github.fushuwei.scaskeleton.system.entity.SysRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色管理 Mapper
 *
 * @author Fu Wei
 */
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 分页查询角色列表
     *
     * @param page     分页对象
     * @param tenantId 租户 ID
     * @param request  查询条件
     * @return 分页结果
     */
    IPage<SysRole> selectRolePage(IPage<SysRole> page, @Param("tenantId") String tenantId, @Param("request") RolePageRequest request);
}
