package io.github.fushuwei.scaskeleton.system.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.infrastructure.entity.SysUser;
import org.apache.ibatis.annotations.Param;

/**
 * 用户 Mapper。
 *
 * @author Fu Wei
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 分页查询用户列表（含部门名称）。
     *
     * @param page     分页参数
     * @param tenantId 租户ID
     * @param username 用户名（可选，模糊匹配）
     * @param nickname 昵称（可选，模糊匹配）
     * @param status   状态（可选）
     * @param deptId   部门ID（可选，精确匹配）
     * @return 分页用户列表
     */
    IPage<SysUser> selectUserPage(IPage<SysUser> page,
                                  @Param("tenantId") String tenantId,
                                  @Param("username") String username,
                                  @Param("nickname") String nickname,
                                  @Param("status") String status,
                                  @Param("deptId") String deptId);
}
