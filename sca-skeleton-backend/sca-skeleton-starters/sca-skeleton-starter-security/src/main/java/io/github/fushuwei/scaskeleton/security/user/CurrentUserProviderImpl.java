package io.github.fushuwei.scaskeleton.security.user;

import io.github.fushuwei.scaskeleton.core.user.CurrentUserProvider;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;

/**
 * 当前用户信息提供者实现类
 *
 * @author Fu Wei
 */
public class CurrentUserProviderImpl implements CurrentUserProvider {

    /**
     * 获取当前请求用户的租户 ID
     *
     * @return 租户 ID，未认证或超管（无固定租户）时返回 null
     */
    @Override
    public String getTenantId() {
        return SecurityUtils.getTenantId();
    }

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
