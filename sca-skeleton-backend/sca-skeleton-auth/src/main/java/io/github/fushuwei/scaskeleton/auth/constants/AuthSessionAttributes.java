package io.github.fushuwei.scaskeleton.auth.constants;

/**
 * Auth 会话属性常量：统一管理 Session 键名，避免多处硬编码。
 *
 * @author Fu Wei
 */
public final class AuthSessionAttributes {

    /** 登录渠道会话键：记录当前会话属于 admin 还是 portal。 */
    public static final String LOGIN_CHANNEL = "SCA_LOGIN_CHANNEL";

    private AuthSessionAttributes() {
    }
}
