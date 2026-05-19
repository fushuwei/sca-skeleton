package io.github.fushuwei.scaskeleton.auth.security;

import io.github.fushuwei.scaskeleton.auth.token.ScaOpaqueAccessTokenClaimsCustomizer;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security UserDetails 实现，扩展了租户、用户类型、权限码等业务属性。
 * <p>
 * 登录时由 {@link ScaUserDetailsService} 构建，并传入 Spring Security 认证上下文。
 * 自定义授权流程中，该对象会被存入 {@code Authentication.getPrincipal()}，
 * 供 {@link ScaOpaqueAccessTokenClaimsCustomizer} 写入不透明访问令牌的自省 claims。
 *
 * @author Fu Wei
 */
@Getter
public class ScaUserDetails implements UserDetails {

    /** 用户 ID（UUID） */
    private final String userId;

    /** 租户 ID */
    private final String tenantId;

    /** 登录用户名 */
    private final String username;

    /** 密码（BCrypt 密文） */
    private final String password;

    /** 昵称 */
    private final String nickname;

    /** 用户类型：superadmin / tenant_admin / dept_admin / normal */
    private final String userType;

    /**
     * 权限码列表（button 类型），如 ["sys:user:list", "sys:role:add"]。
     * 同时作为 Spring Security GrantedAuthority，支持 @PreAuthorize("hasAuthority('xxx')")。
     */
    private final List<String> permissions;

    /** 账号是否可用（status=active 且在有效期内） */
    private final boolean enabled;

    /** 账号是否未锁定（status != locked） */
    private final boolean accountNonLocked;

    /** 账号是否未过期（status != expired 且在有效期内） */
    private final boolean accountNonExpired;

    /** 凭证是否未过期（mustChangePassword=0） */
    private final boolean credentialsNonExpired;

    public ScaUserDetails(String userId, String tenantId, String username, String password,
                          String nickname, String userType, List<String> permissions,
                          boolean enabled, boolean accountNonLocked,
                          boolean accountNonExpired, boolean credentialsNonExpired) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.userType = userType;
        this.permissions = permissions != null ? permissions : List.of();
        this.enabled = enabled;
        this.accountNonLocked = accountNonLocked;
        this.accountNonExpired = accountNonExpired;
        this.credentialsNonExpired = credentialsNonExpired;
    }

    /**
     * 将权限码列表转为 Spring Security GrantedAuthority 集合。
     * 权限码直接作为 authority 字符串，无需 "ROLE_" 前缀。
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
