package io.github.fushuwei.scaskeleton.security.user;

import io.github.fushuwei.scaskeleton.core.user.CurrentUserProvider;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;

/**
 * 基于 Spring Security 资源服务器的当前用户信息提供者
 *
 * @author Fu Wei
 */
public class CurrentUserProviderImpl implements CurrentUserProvider {

    /**
     * 获取当前请求用户的 ID
     *
     * @return 用户 ID，未认证时返回 null
     */
    @Override
    public String getUserId() {
        return SecurityUtils.getUserId();
    }

    /**
     * 获取当前请求用户的用户名
     *
     * @return 用户名，未认证时返回 null
     */
    @Override
    public String getUsername() {
        return SecurityUtils.getUsername();
    }
}
