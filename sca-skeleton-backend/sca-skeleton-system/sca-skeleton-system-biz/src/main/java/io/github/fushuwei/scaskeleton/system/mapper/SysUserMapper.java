package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserPageRequest;
import io.github.fushuwei.scaskeleton.system.api.response.user.UserResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysUser;
import org.apache.ibatis.annotations.Param;

/**
 * 用户 Mapper。
 *
 * @author Fu Wei
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 分页查询用户列表（含部门名称、角色名称，排除密码字段）。
     *
     * @param page     分页参数
     * @param tenantId 租户ID
     * @param req      分页查询请求参数
     * @return 分页用户列表
     */
    IPage<UserResponse> selectUserPage(IPage<UserResponse> page, @Param("tenantId") String tenantId, @Param("req") UserPageRequest req);
}
