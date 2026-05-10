package io.github.fushuwei.sca.auth.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.fushuwei.sca.auth.infrastructure.entity.SysUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户 Mapper（认证服务专用）。
 *
 * @author Fu Wei
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 查询指定用户所持有的所有权限码（button 类型）。
     * <p>
     * 查询链路：sys_user_role → sys_role_permission → sys_permission。
     * 仅返回 {@code type='button'} 且 {@code status='enabled'} 的权限 code，
     * 用于在 JWT claims 中标识该用户可访问的接口资源。
     *
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @return 权限码列表（去重），如 ["sys:user:list", "sys:user:add"]
     */
    List<String> selectPermissionCodesByUserId(@Param("userId") String userId,
                                               @Param("tenantId") String tenantId);
}
