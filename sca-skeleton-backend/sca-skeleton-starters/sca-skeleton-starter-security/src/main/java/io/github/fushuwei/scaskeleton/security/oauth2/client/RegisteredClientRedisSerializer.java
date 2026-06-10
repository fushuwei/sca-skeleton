package io.github.fushuwei.scaskeleton.security.oauth2.client;

import io.github.fushuwei.scaskeleton.security.oauth2.authorization.OAuth2AuthorizationJsonMapperFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.ConfigurationSettingNames;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 将 {@link RegisteredClient} 与 Redis 缓存 JSON 互转。
 * <p>
 * 与 SAS {@link org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository}
 * 一致：仅对 {@code client_settings}/{@code token_settings} 使用 Security Jackson 模块序列化 Map，
 * 其余标量与集合字段用普通 JSON 承载，反序列化时通过 {@link RegisteredClient.Builder} 重建，避免直接反序列化
 * {@link ClientSettings}、{@link TokenSettings} 等无默认构造器的类型。
 *
 * @author Fu Wei
 */
public final class RegisteredClientRedisSerializer {

    /**
     * 当前 Redis 缓存格式版本；变更字段语义时需递增并兼容或失效旧键。
     */
    private static final int FORMAT_VERSION = 1;

    /**
     * 历史整对象缓存根类型标记，命中时视为无效缓存。
     */
    private static final String LEGACY_ROOT_TYPE_MARKER =
            "\"@class\":\"org.springframework.security.oauth2.server.authorization.client.RegisteredClient\"";

    /**
     * 仅序列化 settings Map（与 SAS JDBC 列语义一致）。
     */
    private final JsonMapper settingsJsonMapper;

    /**
     * 序列化/反序列化外层快照 POJO，不启用 default typing。
     */
    private final JsonMapper snapshotJsonMapper;

    /**
     * @param classLoader 加载 Security Jackson 模块的类加载器
     */
    public RegisteredClientRedisSerializer(ClassLoader classLoader) {
        this.settingsJsonMapper = OAuth2AuthorizationJsonMapperFactory.create(classLoader);
        this.snapshotJsonMapper = JsonMapper.builder().build();
    }

    /**
     * 将注册客户端编码为 Redis 缓存 JSON。
     *
     * @param registeredClient 客户端，不可为 null
     * @return JSON 文本
     */
    public String serialize(RegisteredClient registeredClient) {
        Assert.notNull(registeredClient, "registeredClient cannot be null");
        try {
            // 填充快照 POJO，标量与集合字段用普通 JSON
            RegisteredClientCacheSnapshot snapshot = new RegisteredClientCacheSnapshot();
            snapshot.version = FORMAT_VERSION;
            snapshot.id = registeredClient.getId();
            snapshot.clientId = registeredClient.getClientId();
            snapshot.clientIdIssuedAt = toInstantString(registeredClient.getClientIdIssuedAt());
            snapshot.clientSecret = registeredClient.getClientSecret();
            snapshot.clientSecretExpiresAt = toInstantString(registeredClient.getClientSecretExpiresAt());
            snapshot.clientName = registeredClient.getClientName();
            snapshot.clientAuthenticationMethods = toValueList(registeredClient.getClientAuthenticationMethods(),
                    ClientAuthenticationMethod::getValue);
            snapshot.authorizationGrantTypes = toValueList(registeredClient.getAuthorizationGrantTypes(),
                    AuthorizationGrantType::getValue);
            snapshot.redirectUris = new ArrayList<>(registeredClient.getRedirectUris());
            snapshot.postLogoutRedirectUris = new ArrayList<>(registeredClient.getPostLogoutRedirectUris());
            snapshot.scopes = new ArrayList<>(registeredClient.getScopes());
            // settings 与 SAS JDBC 列一致，使用 Security Jackson 模块
            snapshot.clientSettings = this.settingsJsonMapper.writeValueAsString(
                    registeredClient.getClientSettings().getSettings());
            snapshot.tokenSettings = this.settingsJsonMapper.writeValueAsString(
                    registeredClient.getTokenSettings().getSettings());
            return this.snapshotJsonMapper.writeValueAsString(snapshot);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize RegisteredClient for Redis cache", ex);
        }
    }

    /**
     * 从 Redis 缓存 JSON 解码注册客户端。
     *
     * @param json 缓存 JSON；空或历史整对象格式时返回 null
     * @return 客户端；无法识别格式时返回 null
     */
    @Nullable
    public RegisteredClient deserialize(@Nullable String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        // 历史整对象 default typing 格式视为无效，触发回源 JDBC
        if (json.contains(LEGACY_ROOT_TYPE_MARKER)) {
            return null;
        }
        try {
            RegisteredClientCacheSnapshot snapshot = this.snapshotJsonMapper.readValue(json,
                    RegisteredClientCacheSnapshot.class);
            // 版本或必填字段不匹配时拒绝使用该缓存
            if (snapshot == null || snapshot.version != FORMAT_VERSION || !StringUtils.hasText(snapshot.id)
                    || !StringUtils.hasText(snapshot.clientId)) {
                return null;
            }
            return toRegisteredClient(snapshot);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize RegisteredClient from Redis cache", ex);
        }
    }

    /**
     * 由快照字段重建 {@link RegisteredClient}，settings 解析逻辑与 SAS JDBC RowMapper 对齐。
     */
    private RegisteredClient toRegisteredClient(RegisteredClientCacheSnapshot snapshot) throws Exception {
        RegisteredClient.Builder builder = RegisteredClient.withId(snapshot.id)
                .clientId(snapshot.clientId)
                .clientIdIssuedAt(parseInstant(snapshot.clientIdIssuedAt))
                .clientSecret(snapshot.clientSecret)
                .clientSecretExpiresAt(parseInstant(snapshot.clientSecretExpiresAt))
                .clientName(snapshot.clientName);
        if (snapshot.clientAuthenticationMethods != null) {
            builder.clientAuthenticationMethods(methods -> snapshot.clientAuthenticationMethods.forEach(
                    value -> methods.add(resolveClientAuthenticationMethod(value))));
        }
        if (snapshot.authorizationGrantTypes != null) {
            builder.authorizationGrantTypes(grantTypes -> snapshot.authorizationGrantTypes.forEach(
                    value -> grantTypes.add(resolveAuthorizationGrantType(value))));
        }
        if (snapshot.redirectUris != null) {
            builder.redirectUris(uris -> uris.addAll(snapshot.redirectUris));
        }
        if (snapshot.postLogoutRedirectUris != null) {
            builder.postLogoutRedirectUris(uris -> uris.addAll(snapshot.postLogoutRedirectUris));
        }
        if (snapshot.scopes != null) {
            builder.scopes(scopes -> scopes.addAll(snapshot.scopes));
        }
        // client_settings / token_settings 反序列化为 Map 后交给 Builder
        Map<String, Object> clientSettingsMap = readSettingsMap(snapshot.clientSettings);
        builder.clientSettings(ClientSettings.withSettings(clientSettingsMap).build());
        Map<String, Object> tokenSettingsMap = readSettingsMap(snapshot.tokenSettings);
        TokenSettings.Builder tokenSettingsBuilder = TokenSettings.withSettings(tokenSettingsMap);
        // 未显式配置 access_token 格式时，与 SAS JDBC 默认行为一致
        if (!tokenSettingsMap.containsKey(ConfigurationSettingNames.Token.ACCESS_TOKEN_FORMAT)) {
            tokenSettingsBuilder.accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED);
        }
        builder.tokenSettings(tokenSettingsBuilder.build());
        return builder.build();
    }

    /**
     * 使用与 SAS JDBC 相同的 JsonMapper 将 settings JSON 反序列化为 Map。
     */
    private Map<String, Object> readSettingsMap(@Nullable String json) throws Exception {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        ParameterizedTypeReference<Map<String, Object>> typeReference = new ParameterizedTypeReference<>() {
        };
        JavaType javaType = this.settingsJsonMapper.getTypeFactory().constructType(typeReference.getType());
        // 与 SAS JDBC client_settings / token_settings 列反序列化方式一致
        return this.settingsJsonMapper.readValue(json, javaType);
    }

    @Nullable
    private static String toInstantString(@Nullable Instant instant) {
        return instant != null ? instant.toString() : null;
    }

    @Nullable
    private static Instant parseInstant(@Nullable String value) {
        return StringUtils.hasText(value) ? Instant.parse(value) : null;
    }

    private static <T> List<String> toValueList(Set<T> items, java.util.function.Function<T, String> valueExtractor) {
        List<String> values = new ArrayList<>(items.size());
        items.forEach(item -> values.add(valueExtractor.apply(item)));
        return values;
    }

    private static AuthorizationGrantType resolveAuthorizationGrantType(String authorizationGrantType) {
        if (AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(authorizationGrantType)) {
            return AuthorizationGrantType.AUTHORIZATION_CODE;
        }
        if (AuthorizationGrantType.CLIENT_CREDENTIALS.getValue().equals(authorizationGrantType)) {
            return AuthorizationGrantType.CLIENT_CREDENTIALS;
        }
        if (AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(authorizationGrantType)) {
            return AuthorizationGrantType.REFRESH_TOKEN;
        }
        // 自定义授权类型按字符串构造
        return new AuthorizationGrantType(authorizationGrantType);
    }

    private static ClientAuthenticationMethod resolveClientAuthenticationMethod(String clientAuthenticationMethod) {
        if (ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue().equals(clientAuthenticationMethod)) {
            return ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
        }
        if (ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue().equals(clientAuthenticationMethod)) {
            return ClientAuthenticationMethod.CLIENT_SECRET_POST;
        }
        if (ClientAuthenticationMethod.NONE.getValue().equals(clientAuthenticationMethod)) {
            return ClientAuthenticationMethod.NONE;
        }
        // 扩展认证方式按字符串构造
        return new ClientAuthenticationMethod(clientAuthenticationMethod);
    }

    /**
     * Redis 缓存用的注册客户端快照（普通 POJO，供 {@link #snapshotJsonMapper} 读写）。
     */
    private static final class RegisteredClientCacheSnapshot {

        /**
         * 格式版本号。
         */
        public int version;

        /**
         * 客户端主键。
         */
        public String id;

        /**
         * OAuth2 client_id。
         */
        public String clientId;

        /**
         * client_id 签发时间（ISO-8601 字符串）。
         */
        @Nullable
        public String clientIdIssuedAt;

        /**
         * 客户端密钥。
         */
        @Nullable
        public String clientSecret;

        /**
         * 客户端密钥过期时间（ISO-8601 字符串）。
         */
        @Nullable
        public String clientSecretExpiresAt;

        /**
         * 客户端显示名称。
         */
        @Nullable
        public String clientName;

        /**
         * 客户端认证方式取值列表。
         */
        @Nullable
        public List<String> clientAuthenticationMethods;

        /**
         * 授权类型取值列表。
         */
        @Nullable
        public List<String> authorizationGrantTypes;

        /**
         * 重定向 URI 列表。
         */
        @Nullable
        public List<String> redirectUris;

        /**
         * 登出重定向 URI 列表。
         */
        @Nullable
        public List<String> postLogoutRedirectUris;

        /**
         * scope 列表。
         */
        @Nullable
        public List<String> scopes;

        /**
         * client_settings 列同款 JSON（Map 序列化结果）。
         */
        @Nullable
        public String clientSettings;

        /**
         * token_settings 列同款 JSON（Map 序列化结果）。
         */
        @Nullable
        public String tokenSettings;
    }
}
