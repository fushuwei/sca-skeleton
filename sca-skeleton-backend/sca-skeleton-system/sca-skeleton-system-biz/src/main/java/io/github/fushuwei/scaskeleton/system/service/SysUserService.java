package io.github.fushuwei.scaskeleton.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserPasswordResetRequest;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserBatchStatusRequest;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserStatusChangeRequest;
import io.github.fushuwei.scaskeleton.system.api.response.user.UserProfileResponse;
import io.github.fushuwei.scaskeleton.system.api.response.user.UserResponse;

import java.util.List;

/**
 * 用户管理 Service
 *
 * @author Fu Wei
 */
public interface SysUserService {

    /**
     * 获取当前登录用户基本信息
     *
     * @return 当前登录用户基本信息
     */
    UserProfileResponse getUserProfile();

    /**
     * 分页查询用户列表
     *
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<UserResponse> pageUsers(UserPageRequest request);

    /**
     * 统计当前租户用户总数
     *
     * @return 用户总数
     */
    long countUsers();

    /**
     * 根据 ID 查询用户详情
     *
     * @param id 用户 ID
     * @return 用户详情
     */
    UserResponse getUserById(String id);

    /**
     * 新增用户
     *
     * @param request 用户信息
     */
    void createUser(UserCreateRequest request);

    /**
     * 编辑用户
     *
     * @param request 用户信息
     */
    void updateUser(UserUpdateRequest request);

    /**
     * 删除用户
     *
     * @param id 用户 ID
     */
    void deleteUser(String id);

    /**
     * 批量删除用户
     *
     * @param ids 用户 ID 列表
     */
    void batchDeleteUsers(List<String> ids);

    /**
     * 重置密码
     *
     * @param request 密码重置信息
     */
    void resetPassword(UserPasswordResetRequest request);

    /**
     * 变更用户状态
     *
     * @param request 状态变更信息
     */
    void changeStatus(UserStatusChangeRequest request);

    /**
     * 批量变更用户状态
     *
     * @param request 批量状态变更信息
     */
    void batchChangeStatus(UserBatchStatusRequest request);
}
