package io.github.fushuwei.scaskeleton.auth.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.fushuwei.scaskeleton.auth.infrastructure.entity.SysUser;
import io.github.fushuwei.scaskeleton.auth.infrastructure.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户详情服务：从数据库加载用户信息用于 Spring Security 认证。
 * <p>
 * 在自定义密码授权模式（{@code PasswordGrantAuthenticationProvider}）中被调用。
 * 加载链路：sys_user → sys_user_role → sys_role_permission → sys_permission（权限码）。
 * <p>
 * 注意：此接口按 username 查询。多租户场景下 username 可能在不同租户间重复，
 * 实际登录时需结合租户信息；单租户/超管场景可直接按 username 唯一查询。
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScaUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;

    /**
     * 按用户名加载用户详情（仅后台用户）。
     *
     * @param username 登录用户名
     * @return {@link ScaUserDetails}
     * @throws UsernameNotFoundException 用户不存在
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getUserCategory, "backend")
        );
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在：" + username);
        }
        return buildUserDetails(user);
    }

    /**
     * 按用户名 + 租户 ID 加载用户详情（多租户精确查询）。
     *
     * @param username 登录用户名
     * @param tenantId 租户 ID
     * @return {@link ScaUserDetails}
     * @throws UsernameNotFoundException 用户不存在
     */
    public UserDetails loadUserByUsernameAndTenant(String username, String tenantId)
            throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getTenantId, tenantId)
                        .eq(SysUser::getUserCategory, "backend")
        );
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在：" + username);
        }
        return buildUserDetails(user);
    }

    /**
     * 将数据库用户对象转换为 Spring Security UserDetails。
     * 同时加载该用户所有可用的权限码（button 类型）。
     */
    private ScaUserDetails buildUserDetails(SysUser user) {
        List<String> permissions = sysUserMapper.selectPermissionCodesByUserId(
                user.getId(), user.getTenantId());

        LocalDateTime now = LocalDateTime.now();

        // 账号是否可用（status=active 且在有效期内）
        boolean enabled = "active".equals(user.getStatus())
                && (user.getEffectiveStartTime() == null || !now.isBefore(user.getEffectiveStartTime()))
                && (user.getEffectiveEndTime() == null || !now.isAfter(user.getEffectiveEndTime()));

        // 账号是否未锁定
        boolean accountNonLocked = !"locked".equals(user.getStatus())
                && !"frozen".equals(user.getStatus());

        // 账号是否未过期
        boolean accountNonExpired = !"expired".equals(user.getStatus())
                && (user.getEffectiveEndTime() == null || !now.isAfter(user.getEffectiveEndTime()));

        // 凭证是否未过期（mustChangePassword=1 表示必须修改密码，此时拒绝登录）
        boolean credentialsNonExpired = user.getMustChangePassword() == null
                || user.getMustChangePassword() == 0;

        return new ScaUserDetails(
                user.getId(),
                user.getTenantId(),
                user.getUsername(),
                user.getPassword(),
                user.getNickname(),
                user.getUserType(),
                permissions,
                enabled,
                accountNonLocked,
                accountNonExpired,
                credentialsNonExpired
        );
    }
}
