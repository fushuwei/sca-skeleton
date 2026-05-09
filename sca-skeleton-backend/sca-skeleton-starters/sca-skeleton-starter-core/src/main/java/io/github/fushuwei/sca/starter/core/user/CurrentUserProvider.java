package io.github.fushuwei.sca.starter.core.user;

/**
 * 当前用户信息提供接口。
 * <p>
 * 定义获取当前请求用户基础信息的契约，在 {@code starter-core} 中声明，
 * 由 {@code starter-security} 提供基于 Spring Security JWT 的实现，
 * 供 MyBatis-Plus 审计字段自动填充、操作日志等通用能力使用。
 * <p>
 * 未启用 Security Starter 的模块（如纯内部服务）可自行注册实现 Bean。
 *
 * @author Fu Wei
 */
public interface CurrentUserProvider {

    /**
     * 获取当前请求用户的 ID（32 位小写无连字符 UUID）。
     *
     * @return 用户 ID，未认证时返回 {@code null}
     */
    String getCurrentUserId();

    /**
     * 获取当前请求用户的用户名（登录账号）。
     *
     * @return 用户名，未认证时返回 {@code null}
     */
    String getCurrentUsername();
}
