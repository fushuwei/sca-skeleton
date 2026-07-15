package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserPageRequest;
import io.github.fushuwei.scaskeleton.system.api.response.user.UserResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysUser;
import org.apache.ibatis.annotations.Param;

/**
 * 用户管理 Mapper
 *
 * @author Fu Wei
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 分页查询用户列表
     *
     * @param page     分页对象（框架回填）
     * @param tenantId 租户 ID
     * @param request  查询条件
     * @return 分页结果
     */
    IPage<UserResponse> selectUserPage(IPage<UserResponse> page, @Param("tenantId") String tenantId, @Param("request") UserPageRequest request);
}
