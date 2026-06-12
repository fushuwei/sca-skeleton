package io.github.fushuwei.scaskeleton.core.user;

/**
 * 当前用户信息提供者接口
 * <p>
 * 获取当前请求用户的基础信息，供 MyBatis-Plus 审计字段自动填充、操作日志等通用能力使用
 *
 * @author Fu Wei
 */
public interface CurrentUserProvider {

    /**
     * 获取当前请求用户的 ID
     *
     * @return 用户 ID，未认证时返回 null
     */
    String getUserId();

    /**
     * 获取当前请求用户的用户名
     *
     * @return 用户名，未认证时返回 null
     */
    String getUsername();
}
