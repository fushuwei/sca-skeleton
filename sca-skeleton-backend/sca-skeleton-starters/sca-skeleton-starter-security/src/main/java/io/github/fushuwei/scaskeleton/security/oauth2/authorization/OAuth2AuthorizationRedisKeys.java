package io.github.fushuwei.scaskeleton.security.oauth2.authorization;

/**
 * OAuth2 授权与注册客户端在 Redis 中的键名约定。
 * <p>
 * 与 {@link RedisOAuth2AuthorizationService}、{@link RedisRegisteredClientRepository} 读写结构严格一致。
 *
 * @author Fu Wei
 */
public final class OAuth2AuthorizationRedisKeys {

    /**
     * 授权主记录 Hash 前缀，完整键为 {@code sca:oauth2:authorization:{authorizationId}}。
     */
    public static final String AUTH_KEY_PREFIX = "sca:oauth2:authorization:";

    /**
     * 令牌值到授权主键的二级索引前缀，完整键为 {@code sca:oauth2:authorization:idx:{type}:{tokenValue}}。
     */
    public static final String IDX_PREFIX = "sca:oauth2:authorization:idx:";

    /**
     * access_token 在二级索引中的类型片段。
     */
    public static final String IDX_TYPE_ACCESS = "access";

    /**
     * 注册客户端主键缓存前缀，完整键为 {@code sca:oauth2:registered-client:id:{id}}。
     */
    public static final String REGISTERED_CLIENT_ID_PREFIX = "sca:oauth2:registered-client:id:";

    /**
     * client_id 到注册客户端主键的索引前缀，完整键为 {@code sca:oauth2:registered-client:client-id:{clientId}}。
     */
    public static final String REGISTERED_CLIENT_CLIENT_ID_PREFIX = "sca:oauth2:registered-client:client-id:";

    private OAuth2AuthorizationRedisKeys() {
    }

    /**
     * 构造授权主键对应的 Redis Hash 键。
     *
     * @param authorizationId 授权记录主键
     * @return 完整 Redis key
     */
    public static String authorizationKey(String authorizationId) {
        return AUTH_KEY_PREFIX + authorizationId;
    }

    /**
     * 构造 access_token 二级索引键。
     *
     * @param accessTokenValue 不透明 access_token 明文
     * @return 索引 Redis key
     */
    public static String accessTokenIndexKey(String accessTokenValue) {
        return IDX_PREFIX + IDX_TYPE_ACCESS + ":" + accessTokenValue;
    }

    /**
     * 构造注册客户端主键缓存键。
     *
     * @param id 注册客户端主键
     * @return 完整 Redis key
     */
    public static String registeredClientIdKey(String id) {
        return REGISTERED_CLIENT_ID_PREFIX + id;
    }

    /**
     * 构造 client_id 到注册客户端主键的索引键。
     *
     * @param clientId OAuth2 client_id
     * @return 索引 Redis key
     */
    public static String registeredClientClientIdKey(String clientId) {
        return REGISTERED_CLIENT_CLIENT_ID_PREFIX + clientId;
    }
}
