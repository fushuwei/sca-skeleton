package io.github.fushuwei.scaskeleton.security.user;

import io.github.fushuwei.scaskeleton.core.user.CurrentUserProvider;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import io.github.fushuwei.scaskeleton.security.properties.OAuth2ResourceServerProperties;
import lombok.RequiredArgsConstructor;

/**
 * 基于 Spring Security 资源服务器的当前用户信息提供者
 *
 * @author Fu Wei
 */
@RequiredArgsConstructor
public class CurrentUserProviderImpl implements CurrentUserProvider {

    // 注入 Security 配置属性，用于获取自定义的 Claims 字段名配置
    private final OAuth2ResourceServerProperties securityProperties;

    /**
     * 获取当前请求用户的 ID
     *
     * @return 用户 ID，未认证时返回 null
     */
    @Override
    public String getCurrentUserId() {
        return SecurityUtils.getClaim(securityProperties.getUserIdClaimName());
    }

    /**
     * 获取当前请求用户的用户名
     *
     * @return 用户名，未认证时返回 null
     */
    @Override
    public String getCurrentUsername() {
        return SecurityUtils.getClaim(securityProperties.getUsernameClaimName());
    }
}
