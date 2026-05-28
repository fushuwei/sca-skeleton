package io.github.fushuwei.scaskeleton.auth.security;

/**
 * 登录渠道线程上下文：在表单认证链路中传递 admin / portal 标识。
 * <p>
 * 由 {@link io.github.fushuwei.scaskeleton.auth.security.filter.LoginChannelFilter} 写入，
 * {@link io.github.fushuwei.scaskeleton.auth.security.RoutingUserDetailsService} 读取。
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
