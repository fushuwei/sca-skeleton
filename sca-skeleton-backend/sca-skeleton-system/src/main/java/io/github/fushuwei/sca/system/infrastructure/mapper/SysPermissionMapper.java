package io.github.fushuwei.sca.system.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.fushuwei.sca.system.infrastructure.entity.SysPermission;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 权限 Mapper。
 *
 * @author Fu Wei
 */
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 查询指定角色所拥有的权限列表。
     *
     * @param roleIds 角色ID列表
     * @return 权限列表（去重）
     */
    List<SysPermission> selectPermissionsByRoleIds(@Param("roleIds") List<String> roleIds);
}
