package io.github.fushuwei.sca.auth.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 登录主体，封装租户、用户标识与权限集合。
 *
 * @author Fu Wei
 */
@Getter
public class ScaUserDetails implements UserDetails {

    // 用户主键。
    private final String userId;
    // 租户主键。
    private final String tenantId;
    // 登录名。
    private final String username;
    // 加密存储的口令。
    private final String password;
    // 账号是否未锁定等业务状态集合。
    private final boolean active;
    // Spring Security 权限集合。
    private final Collection<? extends GrantedAuthority> authorities;

    // 构造完整用户主体。
    public ScaUserDetails(
            String userId,
            String tenantId,
            String username,
            String password,
            boolean active,
            List<String> permissionCodes) {
        // 保存基础身份字段。
        this.userId = userId;
        // 保存租户上下文。
        this.tenantId = tenantId;
        // 保存登录名。
        this.username = username;
        // 保存口令密文。
        this.password = password;
        // 保存激活标记。
        this.active = active;
        // 将业务权限码映射为 GrantedAuthority。
        this.authorities = permissionCodes.stream()
                .map(code -> new SimpleGrantedAuthority("SCOPE_" + code))
                .collect(Collectors.toSet());
    }

    // 返回权限集合供授权决策使用。
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 直接返回构造阶段生成的集合。
        return authorities;
    }

    // 返回凭据。
    @Override
    public String getPassword() {
        // 密文字符串。
        return password;
    }

    // 返回用户名。
    @Override
    public String getUsername() {
        // 登录名。
        return username;
    }

    // 账号是否未过期，当前与 active 同步。
    @Override
    public boolean isAccountNonExpired() {
        // 未单独建模过期字段时与 active 对齐。
        return active;
    }

    // 账号是否未锁定。
    @Override
    public boolean isAccountNonLocked() {
        // active 为 false 视为不可用。
        return active;
    }

    // 凭证是否未过期。
    @Override
    public boolean isCredentialsNonExpired() {
        // 未单独建模口令过期策略时默认 true。
        return true;
    }

    // 是否启用。
    @Override
    public boolean isEnabled() {
        // 与 active 一致。
        return active;
    }
}
