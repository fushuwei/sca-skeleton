package io.github.fushuwei.scaskeleton.auth.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.fushuwei.scaskeleton.auth.infrastructure.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户管理 Mapper（认证服务）
 *
 * @author Fu Wei
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 查询指定用户的权限编码列表
     *
     * @param tenantId 租户 ID
     * @param userId   用户 ID
     * @return 权限编码列表（去重）
     */
    List<String> selectPermissionCodesByUserId(@Param("tenantId") String tenantId, @Param("userId") String userId);
}
