package io.github.fushuwei.sca.starter.security.constant;

/**
 * 不透明 access_token 自省响应中的业务 claim 字段名。
 * <p>
 * 认证中心写入 token metadata、资源服务器读取 claim 时均使用本常量，禁止在业务代码中散落硬编码字符串。
 *
 * @author Fu Wei
 */
public final class OAuth2AccessTokenClaimNames {

    /**
     * 用户业务主键（UUID 32 位小写）。
     */
    public static final String SUB = "sub";

    /**
     * 登录用户名。
     */
    public static final String PREFERRED_USERNAME = "preferred_username";

    /**
     * 租户 ID。
     */
    public static final String TENANT_ID = "tenant_id";

    /**
     * 用户类型。
     */
    public static final String USER_TYPE = "user_type";

    /**
     * 昵称（仅展示）。
     */
    public static final String NICKNAME = "nickname";

    /**
     * 权限码列表（字符串数组，不带 ROLE_ 前缀）。
     */
    public static final String PERMISSIONS = "permissions";

    private OAuth2AccessTokenClaimNames() {
    }
}
