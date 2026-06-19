package io.github.fushuwei.scaskeleton.security.oauth2.authorization;

import lombok.experimental.UtilityClass;

/**
 * OAuth2 授权与客户端在 Redis 中的键名定义类
 *
 * @author Fu Wei
 */
@UtilityClass
public final class OAuth2AuthorizationRedisKeys {

    /**
     * 授权记录前缀
     */
    public static final String AUTHORIZATION_PREFIX = "oauth2:authorization:";

    /**
     * 令牌到授权主键的索引前缀
     */
    public static final String AUTHORIZATION_INDEX_PREFIX = "oauth2:authorization:index:";

    /**
     * access_token 在索引中的类型片段
     */
    public static final String INDEX_TYPE_ACCESS = "access";

    /**
     * 客户端主键缓存前缀
     */
    public static final String CLIENT_PREFIX = "oauth2:client:";

    /**
     * client_id 到客户端主键的索引前缀
     */
    public static final String CLIENT_INDEX_PREFIX = "oauth2:client:index:client-id:";

    /**
     * 构造授权主键对应的 Redis 键
     *
     * @param authorizationId 授权记录主键
     * @return 完整 Redis key
     */
    public static String authorizationKey(String authorizationId) {
        return AUTHORIZATION_PREFIX + authorizationId;
    }

    /**
     * 构造 access_token 索引键
     *
     * @param accessTokenValue 不透明 access_token 明文
     * @return 索引 Redis key
     */
    public static String accessTokenIndexKey(String accessTokenValue) {
        return AUTHORIZATION_INDEX_PREFIX + INDEX_TYPE_ACCESS + ":" + accessTokenValue;
    }

    /**
     * 构造客户端主键缓存键
     *
     * @param id 客户端主键
     * @return 完整 Redis key
     */
    public static String clientKey(String id) {
        return CLIENT_PREFIX + id;
    }

    /**
     * 构造 client_id 到客户端主键的索引键
     *
     * @param clientId OAuth2 client_id
     * @return 索引 Redis key
     */
    public static String clientIdIndexKey(String clientId) {
        return CLIENT_INDEX_PREFIX + clientId;
    }
}
