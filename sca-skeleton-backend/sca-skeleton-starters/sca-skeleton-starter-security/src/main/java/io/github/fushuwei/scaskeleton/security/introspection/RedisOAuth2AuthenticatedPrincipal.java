package io.github.fushuwei.scaskeleton.security.introspection;

import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Redis 自省产出的 OAuth2 主体实现。
 * <p>
 * 权限列表由 {@link AuthoritiesOpaqueTokenAuthenticationConverter} 从 {@code authorities} claim 单独映射为
 * {@link org.springframework.security.core.GrantedAuthority}，此处 {@link #getAuthorities()} 保持空集合，
 * 与 Spring 默认 HTTP 自省行为一致。
 *
 * @author Fu Wei
 */
public class RedisOAuth2AuthenticatedPrincipal implements OAuth2AuthenticatedPrincipal {

    /**
     * Spring Security 使用的主体名（通常为 sub）。
     */
    private final String name;

    /**
     * 与 HTTP 自省响应一致的 claim 属性表。
     */
    private final Map<String, Object> attributes;

    /**
     * @param name       主体名
     * @param attributes 自省 claims
     */
    public RedisOAuth2AuthenticatedPrincipal(String name, Map<String, Object> attributes) {
        this.name = name;
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        // 返回自省 claims，供 SecurityUtils / CurrentUserProvider 读取
        return this.attributes;
    }

    @Override
    public Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
        // 权限由 AuthoritiesOpaqueTokenAuthenticationConverter 单独映射，此处保持空集合
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return this.name;
    }
}
