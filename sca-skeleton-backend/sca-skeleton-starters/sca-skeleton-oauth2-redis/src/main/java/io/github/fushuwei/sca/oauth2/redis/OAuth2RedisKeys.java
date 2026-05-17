package io.github.fushuwei.sca.oauth2.redis;

/**
 * OAuth2 授权数据在 Redis 中的键名约定。
 * <p>
 * 与 {@code sca-skeleton-auth} 中 {@link io.github.fushuwei.sca.auth.authorization.RedisOAuth2AuthorizationService}
 * 写入的键结构严格一致，供资源服务器只读校验时复用，避免各模块各自维护前缀导致查不到令牌。
 *
 * @author Fu Wei
 */
public final class OAuth2RedisKeys {

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
     * Redis Hash 字段：access_token 过期时间（epoch 毫秒字符串）。
     */
    public static final String FIELD_ACCESS_TOKEN_EXPIRES_AT = "access_token_expires_at";

    /**
     * Redis Hash 字段：access_token 元数据 JSON（含 SAS 写入的 claims）。
     */
    public static final String FIELD_ACCESS_TOKEN_METADATA = "access_token_metadata";

    private OAuth2RedisKeys() {
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
}
