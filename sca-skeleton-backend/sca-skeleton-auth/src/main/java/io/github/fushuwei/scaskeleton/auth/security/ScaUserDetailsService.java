package io.github.fushuwei.scaskeleton.auth.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.fushuwei.scaskeleton.auth.infrastructure.entity.SysUser;
import io.github.fushuwei.scaskeleton.auth.infrastructure.mapper.SysUserMapper;
import io.github.fushuwei.scaskeleton.security.user.ScaUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户详情服务：从数据库加载用户信息用于 Spring Security 表单登录与 OAuth2 令牌颁发。
 * <p>
 * 管理后台加载 {@code user_type=backend}；前台门户加载 {@code user_type=frontend}。
 * 由 {@link RoutingUserDetailsService} 按 {@link LoginChannel} 路由调用。
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScaUserDetailsService {

    private final SysUserMapper sysUserMapper;

    /**
     * 登录失败锁定与自动解锁
     */
    private final LoginAttemptService loginAttemptService;

    /**
     * 按用户名加载后台用户（user_type=backend）。
     *
     * @param username 登录用户名
     * @return {@link ScaUserDetails}
     * @throws UsernameNotFoundException 用户不存在
     */
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 查询后台用户类别
        SysUser user = sysUserMapper.selectOne(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getUserType, "backend")
        );
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在：" + username);
        }
        return buildUserDetails(user);
    }

    /**
     * 按用户名加载前台门户用户（user_type=frontend）。
     *
     * @param username 登录用户名
     * @return {@link ScaUserDetails}
     * @throws UsernameNotFoundException 用户不存在
     */
    public UserDetails loadFrontendUserByUsername(String username) throws UsernameNotFoundException {
        // 查询前台用户类别
        SysUser user = sysUserMapper.selectOne(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getUserType, "frontend")
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
        // 多租户场景：用户名 + 租户 ID + 后台用户类别三重约束
        SysUser user = sysUserMapper.selectOne(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getUserType, "backend")
        );
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在：" + username);
        }
        // 复用统一构建逻辑，附带权限编码与账号状态判定
        return buildUserDetails(user);
    }

    /**
     * 将数据库用户对象转换为 Spring Security UserDetails。
     * 同时加载该用户所有可用的权限编码（menu 和 button 类型）。
     * <p>
     * 平台超级管理员跳过权限查询：RequiresPermissionChecker 对超管直接放行，
     * 且 admin 账号的 tenant_id 为 NULL，SQL 的 tenant_id 条件无法匹配，查询无意义。
     */
    private ScaUserDetails buildUserDetails(SysUser user) {
        // 锁定到期后自动解锁，避免永久 locked
        loginAttemptService.unlockIfExpired(user);

        // 超管跳过权限查询（鉴权时 RequiresPermissionChecker 直接放行，无需加载权限）
        List<String> permissions = (user.getIsSuperadmin() != null && user.getIsSuperadmin() == 1)
            ? List.of() : sysUserMapper.selectPermissionCodesByUserId(user.getTenantId(), user.getId());

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
            user.getIsSuperadmin(),
            permissions,
            enabled,
            accountNonLocked,
            accountNonExpired,
            credentialsNonExpired
        );
    }
}
