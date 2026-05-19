package io.github.fushuwei.scaskeleton.system.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.dto.user.UserPageRequest;
import io.github.fushuwei.scaskeleton.system.api.dto.user.UserSaveRequest;
import io.github.fushuwei.scaskeleton.system.infrastructure.entity.SysUser;

/**
 * 用户管理服务接口。
 *
 * @author Fu Wei
 */
public interface SysUserService {

    /** 分页查询用户 */
    IPage<SysUser> pageUsers(String tenantId, UserPageRequest request);

    /** 根据ID查询用户详情 */
    SysUser getUserById(String id);

    /** 创建用户（含关联部门、岗位、角色） */
    void createUser(String tenantId, UserSaveRequest request);

    /** 更新用户信息 */
    void updateUser(String tenantId, UserSaveRequest request);

    /** 删除用户（逻辑删除，同时清理关联关系） */
    void deleteUser(String id);

    /** 重置密码 */
    void resetPassword(String id, String newPassword);

    /** 修改账号状态（启用/禁用/锁定等） */
    void changeStatus(String id, String status, String reason);
}
