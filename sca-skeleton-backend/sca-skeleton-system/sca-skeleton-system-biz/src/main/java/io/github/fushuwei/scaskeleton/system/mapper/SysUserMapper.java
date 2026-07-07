package io.github.fushuwei.scaskeleton.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.response.user.UserPageResponse;
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
     * @param page           分页参数
     * @param tenantId       租户ID
     * @param username       用户名（可选，模糊匹配）
     * @param nickname       昵称（可选，模糊匹配）
     * @param status         状态（可选）
     * @param deptId         部门ID（可选，精确匹配）
     * @param orderBy        排序字段（可选，白名单校验）
     * @param orderDirection 排序方向 ASC/DESC（可选）
     * @return 分页用户列表（UserPageResponse）
     */
    IPage<UserPageResponse> selectUserPage(IPage<UserPageResponse> page,
                                     @Param("tenantId") String tenantId,
                                     @Param("keyword") String keyword,
                                     @Param("username") String username,
                                     @Param("nickname") String nickname,
                                     @Param("userType") String userType,
                                     @Param("status") String status,
                                     @Param("deptId") String deptId,
                                     @Param("orderBy") String orderBy,
                                     @Param("orderDirection") String orderDirection);
}
