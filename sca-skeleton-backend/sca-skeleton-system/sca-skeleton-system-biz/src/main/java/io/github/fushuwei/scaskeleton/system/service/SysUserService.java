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
     */
    UserProfileResponse getUserProfile();

    /**
     * 分页查询用户列表
     */
    IPage<UserResponse> pageUsers(UserPageRequest request);

    /**
     * 根据 ID 查询用户详情
     */
    UserResponse getUserById(String id);

    /**
     * 新增用户
     */
    void createUser(UserCreateRequest request);

    /**
     * 编辑用户
     */
    void updateUser(UserUpdateRequest request);

    /**
     * 删除用户
     */
    void deleteUser(String id);

    /**
     * 批量删除用户
     */
    void batchDeleteUsers(List<String> ids);

    /**
     * 重置密码
     */
    void resetPassword(UserPasswordResetRequest request);

    /**
     * 变更用户状态
     */
    void changeStatus(UserStatusChangeRequest request);

    /**
     * 批量变更用户状态
     */
    void batchChangeStatus(UserBatchStatusRequest request);
}
