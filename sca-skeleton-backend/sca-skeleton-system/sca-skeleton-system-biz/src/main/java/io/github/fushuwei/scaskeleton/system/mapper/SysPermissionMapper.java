package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.fushuwei.scaskeleton.system.entity.SysPermission;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 权限管理 Mapper
 *
 * @author Fu Wei
 */
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 查询指定角色的权限列表
     *
     * @param roleIds 角色 ID 列表
     * @return 权限列表（去重）
     */
    List<SysPermission> selectPermissionsByRoleIds(@Param("roleIds") List<String> roleIds);
}
