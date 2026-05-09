package io.github.fushuwei.sca.auth.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.fushuwei.sca.auth.infrastructure.entity.SysUser;
import io.github.fushuwei.sca.auth.infrastructure.mapper.PermissionMapper;
import io.github.fushuwei.sca.auth.infrastructure.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 基于 sys_user 与权限表的 UserDetails 加载。
 *
 * @author Fu Wei
 */
@Service
@RequiredArgsConstructor
public class ScaUserDetailsService implements UserDetailsService {

    // 用户表访问。
    private final SysUserMapper sysUserMapper;
    // 权限查询。
    private final PermissionMapper permissionMapper;
    // 登录主体分隔符，避免用户名本身包含冲突字符。
    private static final String SEP = "\u0001";

    // 将租户与用户名编码为单一 principal。
    public static String composePrincipal(String tenantId, String username) {
        // 使用不可见分隔符连接租户与用户名。
        return tenantId + SEP + username;
    }

    // Spring Security 回调，principal 为 tenant\u0001username。
    @Override
    public UserDetails loadUserByUsername(String principal) throws UsernameNotFoundException {
        // 拆分租户与用户名。
        String[] parts = principal.split(SEP, 2);
        // 参数不合法直接拒绝。
        if (parts.length != 2) {
            throw new UsernameNotFoundException("invalid principal");
        }
        // 取出租户与用户名。
        String tenantId = parts[0];
        String username = parts[1];
        // 查询用户行。
        SysUser user = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getUsername, username));
        // 不存在则抛出标准异常。
        if (user == null) {
            throw new UsernameNotFoundException("user not found");
        }
        // 仅允许后台用户走该登录通道。
        if (!"backend".equals(user.getUserCategory())) {
            throw new UsernameNotFoundException("user category not allowed");
        }
        // 仅 active 状态允许登录。
        boolean active = "active".equalsIgnoreCase(user.getStatus());
        // 拉取权限码列表。
        List<String> codes = permissionMapper.selectPermissionCodesByUserId(user.getId());
        // 组装 UserDetails。
        return new ScaUserDetails(
                user.getId(),
                user.getTenantId(),
                user.getUsername(),
                user.getPassword(),
                active,
                codes);
    }
}
