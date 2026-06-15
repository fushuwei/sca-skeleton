package io.github.fushuwei.scaskeleton.security.introspection;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

import java.util.Collection;
import java.util.Map;

/**
 * 基于 Redis 授权记录自省的 {@link OAuth2AuthenticatedPrincipal} 实现类
 *
 * @author Fu Wei
 */
public class RedisOAuth2AuthenticatedPrincipal implements OAuth2AuthenticatedPrincipal {

    /**
     * 自省后的主体名
     */
    private final String name;

    /**
     * 自省后的 claim 属性表
     */
    private final Map<String, Object> attributes;

    /**
     * 自省后的权限集合
     */
    private final Collection<GrantedAuthority> authorities;

    /**
     * 构造 RedisOAuth2AuthenticatedPrincipal 对象
     *
     * @param name        主体名
     * @param attributes  自省 claims
     * @param authorities 自省权限集合
     */
    public RedisOAuth2AuthenticatedPrincipal(String name, Map<String, Object> attributes,
                                             Collection<GrantedAuthority> authorities) {
        this.name = name;
        this.attributes = attributes;
        this.authorities = authorities;
    }

    @Override
    public @NonNull String getName() {
        return this.name;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }
}
