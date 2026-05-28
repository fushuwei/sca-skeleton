package io.github.fushuwei.scaskeleton.auth.security;

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
 * 位于 starter-security，供 Auth 登录与 Redis OAuth2 授权记录反序列化共用。
 *
 * @author Fu Wei
 */
@Getter
public class ScaUserDetails implements UserDetails {

    private final String userId;

    private final String tenantId;

    private final String username;

    private final String password;

    private final String nickname;

    private final String userType;

    private final List<String> permissions;

    private final boolean enabled;

    private final boolean accountNonLocked;

    private final boolean accountNonExpired;

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

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
