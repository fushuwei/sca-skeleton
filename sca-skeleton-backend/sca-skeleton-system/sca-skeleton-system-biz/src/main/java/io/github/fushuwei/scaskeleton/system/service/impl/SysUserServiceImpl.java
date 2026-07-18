package io.github.fushuwei.scaskeleton.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserPasswordResetRequest;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserBatchStatusRequest;
import io.github.fushuwei.scaskeleton.system.api.request.user.UserStatusChangeRequest;
import io.github.fushuwei.scaskeleton.system.api.response.user.UserProfileResponse;
import io.github.fushuwei.scaskeleton.system.api.response.user.UserResponse;
import io.github.fushuwei.scaskeleton.system.converter.UserConverter;
import io.github.fushuwei.scaskeleton.system.entity.SysUser;
import io.github.fushuwei.scaskeleton.system.entity.SysUserDept;
import io.github.fushuwei.scaskeleton.system.entity.SysUserPost;
import io.github.fushuwei.scaskeleton.system.entity.SysUserRole;
import io.github.fushuwei.scaskeleton.system.entity.SysTenant;
import io.github.fushuwei.scaskeleton.system.mapper.SysUserDeptMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysUserMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysUserPostMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysUserRoleMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysTenantMapper;
import io.github.fushuwei.scaskeleton.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 用户管理 Service 实现类
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper userMapper;

    private final SysUserRoleMapper userRoleMapper;

    private final SysUserDeptMapper userDeptMapper;

    private final SysUserPostMapper userPostMapper;

    private final SysTenantMapper tenantMapper;

    private final UserConverter userConverter;

    private final PasswordEncoder passwordEncoder;

    /**
     * 获取当前登录用户基本信息
     *
     * @return 当前登录用户基本信息
     */
    @Override
    public UserProfileResponse getUserProfile() {
        // 获取当前用户 ID
        String userId = SecurityUtils.getUserId();
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录或令牌无效");
        }

        // 根据 ID 查询用户信息
        SysUser user = userMapper.selectById(userId);
        if (user != null) {
            return userConverter.toUserProfileResponse(user);
        }

        // 用户不存在时，从自省结果属性中获取当前登录用户信息
        String username = SecurityUtils.getUsername();
        String nickname = SecurityUtils.getNickname();
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return UserProfileResponse.builder()
            .id(userId)
            .username(username)
            .nickname(StringUtils.hasText(nickname) ? nickname : username)
            .isSuperadmin(SecurityUtils.isSuperAdmin() ? 1 : 0)
            .build();
    }

    /**
     * 分页查询用户列表
     *
     * @param request 查询条件
     * @return 分页结果
     */
    @Override
    public IPage<UserResponse> pageUsers(UserPageRequest request) {
        Page<UserResponse> page = new Page<>(request.getPageNum(), request.getPageSize());
        // 数据隔离：超管看所有租户，非超管只看自己租户
        String tenantId = SecurityUtils.isSuperAdmin() ? null : SecurityUtils.getTenantId();
        return userMapper.selectUserPage(page, tenantId, request);
    }

    /**
     * 根据 ID 查询用户详情
     *
     * @param id 用户 ID
     * @return 用户详情
     */
    @Override
    public UserResponse getUserById(String id) {
        // 加载用户实体
        SysUser user = loadUserEntity(id);

        // 将用户实体转换为响应对象
        UserResponse response = userConverter.toUserResponse(user);

        // 查询关联的部门
        List<SysUserDept> userDepts = userDeptMapper.selectList(new LambdaQueryWrapper<SysUserDept>()
            .eq(SysUserDept::getTenantId, user.getTenantId())
            .eq(SysUserDept::getUserId, user.getId()));
        response.setDeptIds(userDepts.stream().map(SysUserDept::getDeptId).toList());

        // 查询关联的角色
        List<SysUserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
            .eq(SysUserRole::getTenantId, user.getTenantId())
            .eq(SysUserRole::getUserId, user.getId()));
        response.setRoleIds(userRoles.stream().map(SysUserRole::getRoleId).toList());

        // 查询关联的岗位
        List<SysUserPost> userPosts = userPostMapper.selectList(new LambdaQueryWrapper<SysUserPost>()
            .eq(SysUserPost::getTenantId, user.getTenantId())
            .eq(SysUserPost::getUserId, user.getId()));
        response.setPostIds(userPosts.stream().map(SysUserPost::getPostId).toList());

        return response;
    }

    /**
     * 新增用户
     *
     * @param request 用户信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(UserCreateRequest request) {
        // 获取租户 ID（如果是超管创建，该值由前端页面传入，如果是租户内部用户自己创建，则取当前登录人所在租户的 ID）
        String tenantId = resolveTenantId(request.getTenantId());

        // 用户名在同一个租户内唯一
        long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getTenantId, tenantId)
            .eq(SysUser::getUsername, request.getUsername())
            .eq(SysUser::getUserType, "backend"));
        if (count > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "用户名已存在");
        }

        // 封装用户实体
        SysUser user = new SysUser();
        user.setTenantId(tenantId);
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(StringUtils.hasText(request.getPassword()) ? request.getPassword() : "Aa@123456"));
        user.setNickname(request.getNickname());
        user.setRealName(request.getRealName());
        user.setGender(request.getGender());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setUserType("backend");
        user.setIsSuperadmin(resolveIsSuperadmin(request.getIsSuperadmin()));  // 安全防护：仅超级管理员可创建超级管理员账号，非超管强制为 0（防止垂直越权）
        user.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "active");
        user.setLoginFailCount(0);
        user.setMustChangePassword(1);
        user.setEffectiveStartTime(request.getEffectiveStartTime());
        user.setEffectiveEndTime(request.getEffectiveEndTime());
        user.setRemark(request.getRemark());

        // 保存用户
        userMapper.insert(user);

        // 保存关联关系
        saveUserRelations(tenantId, user.getId(), request.getRoleIds(), request.getDeptIds(), request.getPostIds());
    }

    /**
     * 编辑用户
     *
     * @param request 用户信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserUpdateRequest request) {
        // 加载可操作用户实体
        SysUser user = loadOperableUserEntity(request.getId());

        // 更新字段
        user.setNickname(request.getNickname());
        user.setRealName(request.getRealName());
        user.setGender(request.getGender());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setIsSuperadmin(resolveIsSuperadminForUpdate(request.getIsSuperadmin(), user.getIsSuperadmin()));  // 安全防护：仅超级管理员可修改超级管理员标志，非超管强制保持原值（防止垂直越权）
        user.setMustChangePassword(request.getMustChangePassword());
        user.setEffectiveStartTime(request.getEffectiveStartTime());
        user.setEffectiveEndTime(request.getEffectiveEndTime());
        user.setRemark(request.getRemark());

        // 密码非空时加密更新，并记录密码变更时间
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setPasswordUpdateTime(LocalDateTime.now());
        }

        // 更新用户
        userMapper.updateById(user);

        // 删除旧的关联关系，并保存新的关联关系
        deleteUserRelations(user.getTenantId(), request.getId());
        saveUserRelations(user.getTenantId(), request.getId(), request.getRoleIds(), request.getDeptIds(), request.getPostIds());
    }

    /**
     * 删除用户
     *
     * @param id 用户 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(String id) {
        // 加载可操作用户实体
        SysUser user = loadOperableUserEntity(id);

        // 删除用户
        userMapper.deleteById(user.getId());

        // 删除关联关系
        deleteUserRelations(user.getTenantId(), user.getId());
    }

    /**
     * 批量删除用户
     *
     * @param ids 用户 ID 列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteUsers(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        for (String id : ids) {
            deleteUser(id);
        }
    }

    /**
     * 重置密码
     *
     * @param request 密码重置信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(UserPasswordResetRequest request) {
        // 加载可操作用户实体
        SysUser user = loadOperableUserEntity(request.getId());

        // 更新密码
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
            .eq(SysUser::getId, user.getId())
            .set(SysUser::getPassword, passwordEncoder.encode(request.getNewPassword()))
            .set(SysUser::getMustChangePassword, 0)
            .set(SysUser::getPasswordUpdateTime, LocalDateTime.now()));
    }

    /**
     * 变更用户状态
     *
     * @param request 状态变更信息
     */
    @Override
    public void changeStatus(UserStatusChangeRequest request) {
        // 加载可操作用户实体
        SysUser user = loadOperableUserEntity(request.getId());

        // 更新状态及变更时间与原因
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
            .eq(SysUser::getId, user.getId())
            .set(SysUser::getStatus, request.getStatus())
            .set(SysUser::getStatusTime, LocalDateTime.now())
            .set(SysUser::getStatusReason, request.getReason())
        );
    }

    /**
     * 批量变更用户状态
     *
     * @param request 批量状态变更信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchChangeStatus(UserBatchStatusRequest request) {
        List<String> ids = request.getIds();
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        for (String id : ids) {
            UserStatusChangeRequest item = new UserStatusChangeRequest();
            item.setId(id);
            item.setStatus(request.getStatus());
            item.setReason(request.getReason());
            changeStatus(item);
        }
    }

    /**
     * 保存用户与角色、部门和岗位的关联关系
     *
     * @param tenantId 租户 ID
     * @param userId   用户 ID
     * @param roleIds  角色 ID 列表
     * @param deptIds  部门 ID 列表
     * @param postIds  岗位 ID 列表
     */
    private void saveUserRelations(String tenantId, String userId, List<String> roleIds, List<String> deptIds, List<String> postIds) {
        // 保存用户与角色关联关系
        if (!CollectionUtils.isEmpty(roleIds)) {
            roleIds.forEach(roleId -> {
                SysUserRole ur = new SysUserRole();
                ur.setTenantId(tenantId);
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            });
        }
        // 保存用户与部门关联关系
        if (!CollectionUtils.isEmpty(deptIds)) {
            for (String deptId : deptIds) {
                SysUserDept ud = new SysUserDept();
                ud.setTenantId(tenantId);
                ud.setUserId(userId);
                ud.setDeptId(deptId);
                userDeptMapper.insert(ud);
            }
        }
        // 保存用户与岗位关联关系
        if (!CollectionUtils.isEmpty(postIds)) {
            postIds.forEach(postId -> {
                SysUserPost up = new SysUserPost();
                up.setTenantId(tenantId);
                up.setUserId(userId);
                up.setPostId(postId);
                userPostMapper.insert(up);
            });
        }
    }

    /**
     * 删除用户与角色、部门和岗位的关联关系
     *
     * @param tenantId 租户 ID
     * @param userId   用户 ID
     */
    private void deleteUserRelations(String tenantId, String userId) {
        // 删除关联关系
        userRoleMapper.physicalDeleteByUser(tenantId, userId);
        userDeptMapper.physicalDeleteByUser(tenantId, userId);
        userPostMapper.physicalDeleteByUser(tenantId, userId);
    }

    /**
     * 解析创建时的目标租户 ID
     *
     * @param requestTenantId 创建时传入的目标租户 ID（仅当超管创建时才会使用该参数）
     * @return 实际写入用的租户 ID
     */
    private String resolveTenantId(String requestTenantId) {
        if (SecurityUtils.isSuperAdmin()) {
            if (!StringUtils.hasText(requestTenantId)) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "超管创建需指定目标租户");
            }
            SysTenant tenant = tenantMapper.selectById(requestTenantId);
            if (tenant == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "目标租户不存在");
            }
            return requestTenantId;
        }
        return SecurityUtils.getTenantId();
    }

    /**
     * 解析新建用户的 isSuperadmin 字段（防止垂直越权）
     * <p>
     * 仅超级管理员可创建超级管理员账号，非超管传入的 isSuperadmin 值会被强制忽略为 0
     *
     * @param requestIsSuperadmin 请求传入的 isSuperadmin 值
     * @return 实际写入用的 isSuperadmin 值
     */
    private Integer resolveIsSuperadmin(Integer requestIsSuperadmin) {
        if (SecurityUtils.isSuperAdmin()) {
            return requestIsSuperadmin != null ? requestIsSuperadmin : 0;
        }
        return 0;
    }

    /**
     * 解析编辑用户的 isSuperadmin 字段（防止垂直越权）
     * <p>
     * 仅超级管理员可修改超级管理员标志，非超管传入的 isSuperadmin 值会被忽略，保持用户原有的 isSuperadmin 值不变（避免普通用户通过编辑接口提权其他用户）
     *
     * @param requestIsSuperadmin 请求传入的 isSuperadmin 值
     * @param originalIsSuperadmin 用户原有的 isSuperadmin 值
     * @return 实际写入用的 isSuperadmin 值
     */
    private Integer resolveIsSuperadminForUpdate(Integer requestIsSuperadmin, Integer originalIsSuperadmin) {
        if (SecurityUtils.isSuperAdmin()) {
            return requestIsSuperadmin != null ? requestIsSuperadmin : 0;
        }
        return originalIsSuperadmin != null ? originalIsSuperadmin : 0;
    }

    /**
     * 根据 ID 加载用户实体
     *
     * @param id 用户 ID
     * @return 用户实体
     */
    private SysUser loadUserEntity(String id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (!SecurityUtils.isSuperAdmin()
            && !Objects.equals(user.getTenantId(), SecurityUtils.getTenantId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "权限不足，无法操作其他租户的数据");
        }
        return user;
    }

    /**
     * 根据 ID 加载可操作用户实体
     *
     * @param id 用户 ID
     * @return 用户实体
     */
    private SysUser loadOperableUserEntity(String id) {
        SysUser user = loadUserEntity(id);
        if (user.getIsBuiltin() != null && user.getIsBuiltin() == 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "系统内置用户不允许操作");
        }
        return user;
    }
}
