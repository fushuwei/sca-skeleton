package io.github.fushuwei.sca.starter.core.security;

/**
 * JWT 自定义声明名称常量。
 *
 * @author Fu Wei
 */
public final class JwtClaimNames {

    // 用户主键声明名称。
    public static final String USER_ID = "user_id";
    // 用户名声明名称。
    public static final String USERNAME = "username";

    // 私有构造器用于禁止实例化。
    private JwtClaimNames() {
    }
}
