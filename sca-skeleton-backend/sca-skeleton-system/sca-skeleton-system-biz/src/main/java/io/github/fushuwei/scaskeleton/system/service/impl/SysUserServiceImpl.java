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
import io.github.fushuwei.scaskeleton.system.api.request.user.UserUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.user.UserProfileResponse;
import io.github.fushuwei.scaskeleton.system.api.response.user.UserResponse;
import io.github.fushuwei.scaskeleton.system.converter.UserConverter;
import io.github.fushuwei.scaskeleton.system.entity.SysUser;
import io.github.fushuwei.scaskeleton.system.entity.SysUserDept;
import io.github.fushuwei.scaskeleton.system.entity.SysUserPost;
import io.github.fushuwei.scaskeleton.system.entity.SysUserRole;
import io.github.fushuwei.scaskeleton.system.mapper.SysUserDeptMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysUserMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysUserPostMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysUserRoleMapper;
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

/**
 * 用户管理服务实现。
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    /**
     * 用户表 Mapper
     */
    private final SysUserMapper userMapper;

    /**
     * 用户角色关联表 Mapper
     */
    private final SysUserRoleMapper userRoleMapper;

    /**
     * 用户部门关联表 Mapper
     */
    private final SysUserDeptMapper userDeptMapper;

    /**
     * 用户岗位关联表 Mapper
     */
    private final SysUserPostMapper userPostMapper;

    /**
     * 用户映射结构转换器
     */
    private final UserConverter userConverter;

    /**
     * Spring Security 密码加密器
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 获取当前登录用户基本信息
     *
     * @return {@link UserProfileResponse}
     */
    @Override
    public UserProfileResponse getCurrentProfile() {
        // 获取不透明令牌自省后的用户 ID
        String userId = SecurityUtils.getUserId();
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录或令牌无效");
        }
        // 通过 ID 查询用户信息
        SysUser user = userMapper.selectById(userId);
        if (user != null) {
            return userConverter.toUserProfileResponse(user);
        }

        // 用户 ID 不存在时，从自省结果属性中获取当前登录用户信息
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

    @Override
    public IPage<UserResponse> pageUsers(String tenantId, UserPageRequest req) {
        // 按请求参数构造分页对象，返回 UserResponse（含部门名称、角色名称，排除密码）
        Page<UserResponse> page = new Page<>(req.getPageNum(), req.getPageSize());
        return userMapper.selectUserPage(page, tenantId, req);
    }

    @Override
    public UserResponse getUserById(String id) {
        // 按主键查询用户并转换为响应对象
        SysUser user = loadUserEntity(id);
        UserResponse response = userConverter.toUserResponse(user);

        // 查询关联的部门ID列表
        List<SysUserDept> userDepts = userDeptMapper.selectList(new LambdaQueryWrapper<SysUserDept>()
            .eq(SysUserDept::getTenantId, user.getTenantId())
            .eq(SysUserDept::getUserId, user.getId()));
        response.setDeptIds(userDepts.stream()
            .map(SysUserDept::getDeptId)
            .toList());

        // 查询关联的角色ID列表
        List<SysUserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
            .eq(SysUserRole::getTenantId, user.getTenantId())
            .eq(SysUserRole::getUserId, user.getId()));
        response.setRoleIds(userRoles.stream()
            .map(SysUserRole::getRoleId)
            .toList());

        // 查询关联的岗位ID列表
        List<SysUserPost> userPosts = userPostMapper.selectList(new LambdaQueryWrapper<SysUserPost>()
            .eq(SysUserPost::getTenantId, user.getTenantId())
            .eq(SysUserPost::getUserId, user.getId()));
        response.setPostIds(userPosts.stream()
            .map(SysUserPost::getPostId)
            .toList());

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(String tenantId, UserCreateRequest req) {
        // 用户名在同租户内唯一（仅后台用户类别）
        long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getTenantId, tenantId)
            .eq(SysUser::getUsername, req.getUsername())
            .eq(SysUser::getUserType, "backend"));
        if (count > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "用户名已存在");
        }

        // 组装用户实体
        SysUser user = new SysUser();
        user.setTenantId(tenantId);
        user.setUsername(req.getUsername());
        // 密码加密（DelegatingPasswordEncoder 自动添加 {bcrypt} 前缀）
        String rawPwd = StringUtils.hasText(req.getPassword()) ? req.getPassword() : "Aa@123456";
        user.setPassword(passwordEncoder.encode(rawPwd));
        user.setNickname(req.getNickname());
        user.setRealName(req.getRealName());
        user.setGender(req.getGender());
        user.setPhone(req.getPhone());
        user.setEmail(req.getEmail());
        user.setUserType("backend");
        user.setIsSuperadmin(req.getIsSuperadmin() != null ? req.getIsSuperadmin() : 0);
        user.setStatus(StringUtils.hasText(req.getStatus()) ? req.getStatus() : "active");
        user.setLoginFailCount(0);
        user.setMustChangePassword(1);
        user.setEffectiveStartTime(req.getEffectiveStartTime());
        user.setEffectiveEndTime(req.getEffectiveEndTime());
        user.setRemark(req.getRemark());

        // 持久化用户主表
        userMapper.insert(user);
        // 同事务内建立角色、部门、岗位关联
        saveUserRelations(tenantId, user.getId(), req.getRoleIds(), req.getDeptIds(), req.getPostIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(String tenantId, UserUpdateRequest req) {
        // 校验用户存在并加载当前快照
        SysUser existing = loadUserEntity(req.getId());

        // 更新可编辑字段（用户名不可修改）
        existing.setNickname(req.getNickname());
        existing.setRealName(req.getRealName());
        existing.setGender(req.getGender());
        existing.setPhone(req.getPhone());
        existing.setEmail(req.getEmail());
        existing.setIsSuperadmin(req.getIsSuperadmin() != null ? req.getIsSuperadmin() : 0);
        existing.setMustChangePassword(req.getMustChangePassword());
        existing.setEffectiveStartTime(req.getEffectiveStartTime());
        existing.setEffectiveEndTime(req.getEffectiveEndTime());
        existing.setRemark(req.getRemark());

        // 密码非空时加密更新，并记录密码变更时间
        if (StringUtils.hasText(req.getPassword())) {
            existing.setPassword(passwordEncoder.encode(req.getPassword()));
            existing.setPasswordUpdateTime(LocalDateTime.now());
        }

        userMapper.updateById(existing);

        // 清除旧关联，重新建立
        deleteUserRelations(tenantId, req.getId());
        saveUserRelations(tenantId, req.getId(), req.getRoleIds(), req.getDeptIds(), req.getPostIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(String id) {
        // 加载待删用户并校验内置保护
        SysUser user = loadUserEntity(id);
        if (user.getIsBuiltin() != null && user.getIsBuiltin() == 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "系统内置用户不允许删除");
        }
        // 逻辑删除用户主表
        userMapper.deleteById(id);
        // 同事务内清理角色、部门关联
        deleteUserRelations(user.getTenantId(), id);
    }

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String id, String newPassword) {
        // 校验用户存在
        loadUserEntity(id);
        // 更新密码并清除强制改密标记
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
            .eq(SysUser::getId, id)
            .set(SysUser::getPassword,
                passwordEncoder.encode(newPassword))
            .set(SysUser::getMustChangePassword, 0)
            .set(SysUser::getPasswordUpdateTime, LocalDateTime.now())
        );
    }

    @Override
    public void changeStatus(String id, String status, String reason) {
        // 加载用户并校验内置保护
        SysUser user = loadUserEntity(id);
        if (user.getIsBuiltin() != null && user.getIsBuiltin() == 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "系统内置用户不允许操作");
        }
        // 更新状态及变更时间与原因
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
            .eq(SysUser::getId, id)
            .set(SysUser::getStatus, status)
            .set(SysUser::getStatusTime, LocalDateTime.now())
            .set(SysUser::getStatusReason, reason)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchChangeStatus(List<String> ids, String status, String reason) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        for (String id : ids) {
            changeStatus(id, status, reason);
        }
    }

    /**
     * 保存用户-角色、用户-部门、用户-岗位关联。
     */
    private void saveUserRelations(String tenantId, String userId, List<String> roleIds,
                                   List<String> deptIds, List<String> postIds) {
        // 批量插入用户-角色关联
        if (!CollectionUtils.isEmpty(roleIds)) {
            roleIds.forEach(roleId -> {
                SysUserRole ur = new SysUserRole();
                ur.setTenantId(tenantId);
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            });
        }
        // 批量插入用户-部门关联
        if (!CollectionUtils.isEmpty(deptIds)) {
            for (String deptId : deptIds) {
                SysUserDept ud = new SysUserDept();
                ud.setTenantId(tenantId);
                ud.setUserId(userId);
                ud.setDeptId(deptId);
                userDeptMapper.insert(ud);
            }
        }
        // 批量插入用户-岗位关联
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
     * 物理删除用户的所有角色、部门和岗位关联（关联表为纯关系数据，无需逻辑删除）。
     */
    private void deleteUserRelations(String tenantId, String userId) {
        userRoleMapper.physicalDeleteByUser(tenantId, userId);
        userDeptMapper.physicalDeleteByUser(tenantId, userId);
        userPostMapper.physicalDeleteByUser(tenantId, userId);
    }

    /**
     * 按主键加载用户实体（供内部业务逻辑使用，不对外暴露 Entity）。
     *
     * @param id 用户 ID
     * @return 用户实体
     * @throws BusinessException 用户不存在时抛出 NOT_FOUND
     */
    private SysUser loadUserEntity(String id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }
}
