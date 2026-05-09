package io.github.fushuwei.sca.auth.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限码查询，按用户聚合角色与菜单权限。
 *
 * @author Fu Wei
 */
@Mapper
public interface PermissionMapper {

    // 查询用户具备的全部权限标识，用于 JWT scope 与网关鉴权。
    @Select("""
            SELECT DISTINCT p.code
            FROM sys_permission p
                     INNER JOIN sys_role_permission rp
                                ON p.id = rp.permission_id AND rp.is_deleted = 0
                     INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id AND ur.is_deleted = 0
            WHERE ur.user_id = #{userId}
              AND p.is_deleted = 0
              AND p.code IS NOT NULL
              AND p.code <> ''
              AND p.status = 'enabled'
            """)
    List<String> selectPermissionCodesByUserId(@Param("userId") String userId);
}
