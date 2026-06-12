package io.github.fushuwei.scaskeleton.security.constant;

import lombok.experimental.UtilityClass;

/**
 * 不透明访问令牌自省响应中的 claim 字段名常量类
 *
 * @author Fu Wei
 */
@UtilityClass
public class OAuth2AccessTokenClaimNames {

    /**
     * 租户 ID
     */
    public static final String TENANT_ID = "tenant_id";

    /**
     * 用户 ID
     */
    public static final String SUB = "sub";

    /**
     * 用户名
     */
    public static final String PREFERRED_USERNAME = "preferred_username";

    /**
     * 用户类型
     */
    public static final String USER_TYPE = "user_type";

    /**
     * 用户昵称
     */
    public static final String NICKNAME = "nickname";

    /**
     * 权限编码
     */
    public static final String AUTHORITIES = "authorities";
}
