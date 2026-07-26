package io.github.fushuwei.scaskeleton.auth.security;

/**
 * 登录渠道线程上下文：在密码模式认证链路中传递 admin / portal 标识。
 * <p>
 * 由 {@link io.github.fushuwei.scaskeleton.auth.grant.base.OAuth2ResourceOwnerBaseAuthenticationProvider}
 * 在 {@code authenticate()} 方法中写入，{@link RoutingUserDetailsService} 读取。
 * <p>
 * Provider 在认证成功/失败时直接发布事件（仍在 finally 清理 ThreadLocal 之前），
 * 确保 {@link LoginLogPublisher} 和 {@link LoginAttemptEventListener} 能读取到正确的渠道。
 *
 * @author Fu Wei
 */
public final class LoginChannelContext {

    /** 当前线程绑定的登录渠道 */
    private static final ThreadLocal<LoginChannel> CURRENT = new ThreadLocal<>();

    private LoginChannelContext() {
    }

    /**
     * 绑定当前请求的登录渠道。
     *
     * @param channel admin 或 portal
     */
    public static void set(LoginChannel channel) {
        // ThreadLocal 仅在本请求线程内有效，避免跨请求污染
        CURRENT.set(channel);
    }

    /**
     * @return 当前登录渠道；未设置时返回 null
     */
    public static LoginChannel get() {
        return CURRENT.get();
    }

    /** 清理线程变量，防止线程池复用时串号。 */
    public static void clear() {
        CURRENT.remove();
    }
}
