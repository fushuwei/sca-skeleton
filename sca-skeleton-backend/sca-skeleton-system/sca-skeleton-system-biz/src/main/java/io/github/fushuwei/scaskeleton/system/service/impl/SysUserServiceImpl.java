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
import io.github.fushuwei.scaskeleton.system.entity.SysRole;
import io.github.fushuwei.scaskeleton.system.entity.SysDept;
import io.github.fushuwei.scaskeleton.system.entity.SysPost;
import io.github.fushuwei.scaskeleton.system.entity.SysTenant;
import io.github.fushuwei.scaskeleton.system.mapper.SysUserDeptMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysUserMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysTenantMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysUserPostMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysUserRoleMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysRoleMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysDeptMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysPostMapper;
import io.github.fushuwei.scaskeleton.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    private final SysRoleMapper roleMapper;

    private final SysDeptMapper deptMapper;

    private final SysPostMapper postMapper;

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
            UserProfileResponse profile = userConverter.toUserProfileResponse(user);
            fillTenantInfo(profile, user.getTenantId());
            return profile;
        }

        // 用户不存在时，从自省结果属性中获取当前登录用户信息
        String username = SecurityUtils.getUsername();
        String nickname = SecurityUtils.getNickname();
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        UserProfileResponse profile = UserProfileResponse.builder()
            .id(userId)
            .username(username)
            .nickname(StringUtils.hasText(nickname) ? nickname : username)
            .isSuperadmin(SecurityUtils.isSuperAdmin() ? 1 : 0)
            .build();
        fillTenantInfo(profile, SecurityUtils.getTenantId());
        return profile;
    }

    /**
     * 补充当前登录用户的租户信息（租户 ID 与租户名称）
     *
     * @param profile  用户基本信息响应
     * @param tenantId 租户 ID
     */
    private void fillTenantInfo(UserProfileResponse profile, String tenantId) {
        if (profile == null || !StringUtils.hasText(tenantId)) {
            return;
        }
        profile.setTenantId(tenantId);
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant != null) {
            profile.setTenantName(tenant.getName());
        }
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
        // 数据隔离：仅查询当前租户下的用户
        String tenantId = SecurityUtils.getTenantId();
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
        // 获取当前登录用户所在租户的 ID
        String tenantId = SecurityUtils.getTenantId();

        // 用户名在同一个租户、同一用户域内唯一
        long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getTenantId, tenantId)
            .eq(SysUser::getUsername, request.getUsername())
            .eq(SysUser::getRealm, request.getRealm()));
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
        user.setRealm(request.getRealm());
        user.setIsSuperadmin(resolveIsSuperadmin(request.getIsSuperadmin()));  // 安全防护：仅超级管理员可创建超级管理员账号，非超管强制为 0（防止垂直越权）
        user.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "active");
        user.setLoginFailCount(0);
        user.setMustChangePassword(1);
        user.setEffectiveStartTime(request.getEffectiveStartTime());
        user.setEffectiveEndTime(request.getEffectiveEndTime());
        user.setRemark(request.getRemark());

        // 保存用户
        userMapper.insert(user);

        // 越权防护：校验所选角色、部门、岗位均属于当前租户，且角色域与用户域一致
        validateRoleRealm(tenantId, request.getRoleIds(), request.getRealm());
        validateDeptTenant(tenantId, request.getDeptIds());
        validatePostTenant(tenantId, request.getPostIds());

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

        // 乐观锁：使用前端回传的 version 作为 WHERE 条件，若版本不匹配则影响行数为 0，说明数据已被其他用户修改
        user.setVersion(request.getVersion());
        int affectedRows = userMapper.updateById(user);
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.VERSION_CONFLICT);
        }

        // 删除旧的关联关系，并保存新的关联关系
        deleteUserRelations(user.getTenantId(), request.getId());

        // 越权防护：校验所选角色、部门、岗位均属于用户所在租户，且角色域与用户域一致（用户域不可修改，以加载实体的 realm 为准）
        validateRoleRealm(user.getTenantId(), request.getRoleIds(), user.getRealm());
        validateDeptTenant(user.getTenantId(), request.getDeptIds());
        validatePostTenant(user.getTenantId(), request.getPostIds());

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

        // 批量加载用户实体并校验存在、租户隔离与内置用户
        List<SysUser> users = loadOperableUserEntities(ids);

        // 批量删除关联关系
        deleteUserRelations(users);

        // 批量删除用户
        userMapper.deleteBatchIds(ids);
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
     * 校验所选角色属于当前租户且与用户域一致，防止跨租户/跨域分配角色（越权防护）
     *
     * @param tenantId 当前租户 ID
     * @param roleIds  角色 ID 列表
     * @param realm    用户域
     */
    private void validateRoleRealm(String tenantId, List<String> roleIds, String realm) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        Set<String> roleIdSet = new HashSet<>(roleIds);
        long validCount = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
            .in(SysRole::getId, roleIdSet)
            .eq(SysRole::getTenantId, tenantId)
            .eq(SysRole::getRealm, realm));
        if (validCount != roleIdSet.size()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "所选角色不属于当前租户或与用户域不一致，不允许跨租户/跨域分配角色");
        }
    }

    /**
     * 校验所选部门属于当前租户，防止跨租户分配部门（越权防护）
     *
     * @param tenantId 当前租户 ID
     * @param deptIds  部门 ID 列表
     */
    private void validateDeptTenant(String tenantId, List<String> deptIds) {
        if (CollectionUtils.isEmpty(deptIds)) {
            return;
        }
        Set<String> deptIdSet = new HashSet<>(deptIds);
        long validCount = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>()
            .in(SysDept::getId, deptIdSet)
            .eq(SysDept::getTenantId, tenantId));
        if (validCount != deptIdSet.size()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "所选部门不属于当前租户，不允许跨租户分配部门");
        }
    }

    /**
     * 校验所选岗位属于当前租户，防止跨租户分配岗位（越权防护）
     *
     * @param tenantId 当前租户 ID
     * @param postIds  岗位 ID 列表
     */
    private void validatePostTenant(String tenantId, List<String> postIds) {
        if (CollectionUtils.isEmpty(postIds)) {
            return;
        }
        Set<String> postIdSet = new HashSet<>(postIds);
        long validCount = postMapper.selectCount(new LambdaQueryWrapper<SysPost>()
            .in(SysPost::getId, postIdSet)
            .eq(SysPost::getTenantId, tenantId));
        if (validCount != postIdSet.size()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "所选岗位不属于当前租户，不允许跨租户分配岗位");
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
     * 批量删除多个用户与角色、部门和岗位的关联关系
     * <p>
     * 按 tenantId 分组处理，保证关联表 tenantId 与主表一致（符合多租户硬约束：关联表 tenantId 必须从主实体派生）
     *
     * @param users 用户实体列表
     */
    private void deleteUserRelations(List<SysUser> users) {
        if (CollectionUtils.isEmpty(users)) {
            return;
        }
        // 按 tenantId 分组，收集每个租户下的用户 ID 列表
        Map<String, List<String>> tenantToUserIds = users.stream()
            .collect(Collectors.groupingBy(SysUser::getTenantId,
                Collectors.mapping(SysUser::getId, Collectors.toList())));
        tenantToUserIds.forEach((tenantId, userIds) -> {
            userRoleMapper.physicalDeleteByUsers(tenantId, userIds);
            userDeptMapper.physicalDeleteByUsers(tenantId, userIds);
            userPostMapper.physicalDeleteByUsers(tenantId, userIds);
        });
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
     * @param requestIsSuperadmin  请求传入的 isSuperadmin 值
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
        // 数据隔离：仅允许操作当前租户下的用户
        if (!Objects.equals(user.getTenantId(), SecurityUtils.getTenantId())) {
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

    /**
     * 根据 ID 列表批量加载用户实体并校验存在性、租户隔离与内置用户
     *
     * @param ids 用户 ID 列表
     * @return 用户实体列表
     */
    private List<SysUser> loadOperableUserEntities(List<String> ids) {
        List<String> distinctIds = ids.stream().distinct().toList();
        List<SysUser> entities = userMapper.selectBatchIds(distinctIds);
        if (entities.size() != distinctIds.size()) {
            Set<String> foundIds = entities.stream().map(SysUser::getId).collect(Collectors.toSet());
            List<String> missing = distinctIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在，ID: " + String.join(", ", missing));
        }
        for (SysUser entity : entities) {
            if (!Objects.equals(entity.getTenantId(), SecurityUtils.getTenantId())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "权限不足，无法操作其他租户的数据");
            }
            if (entity.getIsBuiltin() != null && entity.getIsBuiltin() == 1) {
                throw new BusinessException(ResultCode.FORBIDDEN, "系统内置用户不允许操作");
            }
        }
        return entities;
    }
}
