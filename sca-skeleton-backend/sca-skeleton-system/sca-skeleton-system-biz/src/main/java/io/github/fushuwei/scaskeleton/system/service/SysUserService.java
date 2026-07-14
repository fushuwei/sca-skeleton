package io.github.fushuwei.scaskeleton.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.user.UserPageResponse;
import io.github.fushuwei.scaskeleton.system.api.response.user.UserProfileResponse;
import io.github.fushuwei.scaskeleton.system.api.response.user.UserResponse;

import java.util.List;

/**
 * 用户管理服务接口
 *
 * @author Fu Wei
 */
public interface SysUserService {

    /**
     * 获取当前登录用户基本信息
     */
    UserProfileResponse getCurrentProfile();

    /**
     * 分页查询用户（含部门名称、角色名称，排除密码）
     */
    IPage<UserPageResponse> pageUsers(String tenantId, UserPageRequest request);

    /**
     * 根据ID查询用户详情
     */
    UserResponse getUserById(String id);

    /**
     * 创建用户（含关联部门、岗位、角色）
     */
    void createUser(String tenantId, UserCreateRequest request);

    /**
     * 更新用户信息
     */
    void updateUser(String tenantId, UserUpdateRequest request);

    /**
     * 删除用户（逻辑删除，同时清理关联关系）
     */
    void deleteUser(String id);

    /**
     * 批量删除用户
     */
    void batchDeleteUsers(List<String> ids);

    /**
     * 重置密码
     */
    void resetPassword(String id, String newPassword);

    /**
     * 修改账号状态（启用/禁用/锁定等）
     */
    void changeStatus(String id, String status, String reason);

    /**
     * 批量修改账号状态
     */
    void batchChangeStatus(List<String> ids, String status, String reason);
}
