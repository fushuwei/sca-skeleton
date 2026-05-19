package io.github.fushuwei.scaskeleton.system.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.exception.ErrorCode;
import io.github.fushuwei.scaskeleton.system.api.dto.user.UserPageRequest;
import io.github.fushuwei.scaskeleton.system.api.dto.user.UserSaveRequest;
import io.github.fushuwei.scaskeleton.system.application.service.SysUserService;
import io.github.fushuwei.scaskeleton.system.infrastructure.entity.SysUser;
import io.github.fushuwei.scaskeleton.system.infrastructure.entity.SysUserDept;
import io.github.fushuwei.scaskeleton.system.infrastructure.entity.SysUserRole;
import io.github.fushuwei.scaskeleton.system.infrastructure.mapper.SysUserDeptMapper;
import io.github.fushuwei.scaskeleton.system.infrastructure.mapper.SysUserMapper;
import io.github.fushuwei.scaskeleton.system.infrastructure.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 用户管理服务实现。
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
    private final PasswordEncoder passwordEncoder;

    @Override
    public IPage<SysUser> pageUsers(String tenantId, UserPageRequest req) {
        Page<SysUser> page = new Page<>(req.getPageNum(), req.getPageSize());
        return userMapper.selectUserPage(page, tenantId,
                req.getUsername(), req.getNickname(), req.getStatus(), req.getDeptId());
    }

    @Override
    public SysUser getUserById(String id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(String tenantId, UserSaveRequest req) {
        // 用户名在同租户内唯一
        long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getUsername, req.getUsername())
                .eq(SysUser::getUserCategory, "backend"));
        if (count > 0) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setTenantId(tenantId);
        user.setUsername(req.getUsername());
        // 密码加密（使用 DelegatingPasswordEncoder 格式：{bcrypt}...）
        String rawPwd = StringUtils.hasText(req.getPassword()) ? req.getPassword() : "Aa@123456";
        user.setPassword("{bcrypt}" + passwordEncoder.encode(rawPwd));
        user.setNickname(req.getNickname());
        user.setRealName(req.getRealName());
        user.setGender(req.getGender());
        user.setPhone(req.getPhone());
        user.setEmail(req.getEmail());
        user.setUserCategory("backend");
        user.setUserType(req.getUserType());
        user.setStatus(StringUtils.hasText(req.getStatus()) ? req.getStatus() : "active");
        user.setLoginFailCount(0);
        user.setMustChangePassword(1);
        user.setEffectiveStartTime(req.getEffectiveStartTime());
        user.setEffectiveEndTime(req.getEffectiveEndTime());
        user.setRemark(req.getRemark());

        userMapper.insert(user);
        saveUserRelations(tenantId, user.getId(), req);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(String tenantId, UserSaveRequest req) {
        SysUser existing = getUserById(req.getId());

        // 内置用户不允许修改用户名
        if (existing.getIsBuiltin() != null && existing.getIsBuiltin() == 1) {
            req.setUsername(existing.getUsername());
        }

        existing.setNickname(req.getNickname());
        existing.setRealName(req.getRealName());
        existing.setGender(req.getGender());
        existing.setPhone(req.getPhone());
        existing.setEmail(req.getEmail());
        existing.setUserType(req.getUserType());
        existing.setEffectiveStartTime(req.getEffectiveStartTime());
        existing.setEffectiveEndTime(req.getEffectiveEndTime());
        existing.setRemark(req.getRemark());

        userMapper.updateById(existing);

        // 清除旧关联，重新建立
        deleteUserRelations(tenantId, req.getId());
        saveUserRelations(tenantId, req.getId(), req);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(String id) {
        SysUser user = getUserById(id);
        if (user.getIsBuiltin() != null && user.getIsBuiltin() == 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "系统内置用户不允许删除");
        }
        userMapper.deleteById(id);
        deleteUserRelations(user.getTenantId(), id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String id, String newPassword) {
        getUserById(id);
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .set(SysUser::getPassword,
                        "{bcrypt}" + passwordEncoder.encode(newPassword))
                .set(SysUser::getMustChangePassword, 0)
                .set(SysUser::getPasswordUpdateTime, LocalDateTime.now())
        );
    }

    @Override
    public void changeStatus(String id, String status, String reason) {
        SysUser user = getUserById(id);
        if (user.getIsBuiltin() != null && user.getIsBuiltin() == 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "系统内置用户不允许操作");
        }
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .set(SysUser::getStatus, status)
                .set(SysUser::getStatusTime, LocalDateTime.now())
                .set(SysUser::getStatusReason, reason)
        );
    }

    /** 保存用户-角色、用户-部门关联。 */
    private void saveUserRelations(String tenantId, String userId, UserSaveRequest req) {
        if (!CollectionUtils.isEmpty(req.getRoleIds())) {
            req.getRoleIds().forEach(roleId -> {
                SysUserRole ur = new SysUserRole();
                ur.setTenantId(tenantId);
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            });
        }
        if (!CollectionUtils.isEmpty(req.getDeptIds())) {
            for (int i = 0; i < req.getDeptIds().size(); i++) {
                SysUserDept ud = new SysUserDept();
                ud.setTenantId(tenantId);
                ud.setUserId(userId);
                ud.setDeptId(req.getDeptIds().get(i));
                ud.setIsPrimary(i == 0 ? 1 : 0);
                userDeptMapper.insert(ud);
            }
        }
    }

    /** 逻辑删除用户的所有角色和部门关联。 */
    private void deleteUserRelations(String tenantId, String userId) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getTenantId, tenantId)
                .eq(SysUserRole::getUserId, userId));
        userDeptMapper.delete(new LambdaQueryWrapper<SysUserDept>()
                .eq(SysUserDept::getTenantId, tenantId)
                .eq(SysUserDept::getUserId, userId));
    }
}
