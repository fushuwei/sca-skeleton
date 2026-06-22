package io.github.fushuwei.scaskeleton.security.oauth2.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.ConfigurationSettingNames;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 注册客户端 Redis 序列化器
 *
 * @author Fu Wei
 */
@RequiredArgsConstructor
public final class RegisteredClientRedisSerializer {

    /**
     * 当前 Redis 缓存中的 RegisteredClientSnapshot 版本，变更字段语义时需递增并兼容或失效旧键
     */
    private static final int FORMAT_VERSION = 1;

    /**
     * 全局通用 JsonMapper，用于序列化快照 POJO（仅含基础类型字段，无需定制模块）
     */
    private final JsonMapper jsonMapper;

    /**
     * OAuth2 持久层专用 JsonMapper，用于序列化 client_settings / token_settings
     */
    @Qualifier("securityJsonMapper")
    private final JsonMapper securityJsonMapper;

    /**
     * 将 RegisteredClient 转成 RegisteredClientSnapshot 类型，然后序列化成 JSON 字符串，用于后续存储至 Redis 缓存
     *
     * @param registeredClient 注册客户端
     * @return JSON 字符串
     */
    public String serialize(RegisteredClient registeredClient) {
        try {
            RegisteredClientSnapshot snapshot = new RegisteredClientSnapshot();
            snapshot.version = FORMAT_VERSION;
            snapshot.id = registeredClient.getId();
            snapshot.clientId = registeredClient.getClientId();
            snapshot.clientIdIssuedAt = toInstantString(registeredClient.getClientIdIssuedAt());
            snapshot.clientSecret = registeredClient.getClientSecret();
            snapshot.clientSecretExpiresAt = toInstantString(registeredClient.getClientSecretExpiresAt());
            snapshot.clientName = registeredClient.getClientName();
            snapshot.clientAuthenticationMethods = toValueList(registeredClient.getClientAuthenticationMethods(), ClientAuthenticationMethod::getValue);
            snapshot.authorizationGrantTypes = toValueList(registeredClient.getAuthorizationGrantTypes(), AuthorizationGrantType::getValue);
            snapshot.redirectUris = new ArrayList<>(registeredClient.getRedirectUris());
            snapshot.postLogoutRedirectUris = new ArrayList<>(registeredClient.getPostLogoutRedirectUris());
            snapshot.scopes = new ArrayList<>(registeredClient.getScopes());
            // client_settings 和 token_settings 需要使用 OAuth2 持久层专用 JsonMapper 进行序列化
            snapshot.clientSettings = this.securityJsonMapper.writeValueAsString(registeredClient.getClientSettings().getSettings());
            snapshot.tokenSettings = this.securityJsonMapper.writeValueAsString(registeredClient.getTokenSettings().getSettings());
            return this.jsonMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            throw new IllegalStateException("RegisteredClientSnapshot 序列化异常", e);
        }
    }

    /**
     * 将 JSON 反序列化成 RegisteredClientSnapshot，然后将 RegisteredClientSnapshot 转成 RegisteredClient 类型
     *
     * @param json JSON 字符串
     * @return 注册客户端
     */
    public RegisteredClient deserialize(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            RegisteredClientSnapshot snapshot = this.jsonMapper.readValue(json, RegisteredClientSnapshot.class);
            // 判断必填字段是否为空，以及 Redis 缓存中的快照版本是否与当前最新版本一致
            if (snapshot == null || !StringUtils.hasText(snapshot.id) || !StringUtils.hasText(snapshot.clientId)
                || snapshot.version != FORMAT_VERSION) {
                return null;
            }
            return toRegisteredClient(snapshot);
        } catch (Exception e) {
            throw new IllegalStateException("RegisteredClientSnapshot 反序列化异常", e);
        }
    }

    /**
     * 将 RegisteredClientSnapshot 转成 RegisteredClient 类型
     *
     * @param snapshot RegisteredClientSnapshot 对象
     * @return RegisteredClient 对象
     */
    private RegisteredClient toRegisteredClient(RegisteredClientSnapshot snapshot) throws Exception {
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

        Map<String, Object> clientSettingsMap = readSettingsMap(snapshot.clientSettings);
        builder.clientSettings(ClientSettings.withSettings(clientSettingsMap).build());

        Map<String, Object> tokenSettingsMap = readSettingsMap(snapshot.tokenSettings);
        TokenSettings.Builder tokenSettingsBuilder = TokenSettings.withSettings(tokenSettingsMap);
        if (!tokenSettingsMap.containsKey(ConfigurationSettingNames.Token.ACCESS_TOKEN_FORMAT)) {
            tokenSettingsBuilder.accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED);
        }
        builder.tokenSettings(tokenSettingsBuilder.build());

        return builder.build();
    }

    private Map<String, Object> readSettingsMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        ParameterizedTypeReference<Map<String, Object>> typeReference = new ParameterizedTypeReference<>() {};
        JavaType javaType = this.securityJsonMapper.getTypeFactory().constructType(typeReference.getType());
        return this.securityJsonMapper.readValue(json, javaType);
    }

    private static String toInstantString(Instant instant) {
        return instant != null ? instant.toString() : null;
    }

    private static Instant parseInstant(String value) {
        return StringUtils.hasText(value) ? Instant.parse(value) : null;
    }

    private static <T> List<String> toValueList(Set<T> items, Function<T, String> valueExtractor) {
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
     * Redis 缓存用的注册客户端快照
     */
    private static final class RegisteredClientSnapshot {

        /**
         * 快照版本号
         */
        public int version;

        /**
         * 客户端主键
         */
        public String id;

        /**
         * OAuth2 client_id
         */
        public String clientId;

        /**
         * client_id 签发时间
         */
        public String clientIdIssuedAt;

        /**
         * 客户端密钥
         */
        public String clientSecret;

        /**
         * 客户端密钥过期时间
         */
        public String clientSecretExpiresAt;

        /**
         * 客户端显示名称
         */
        public String clientName;

        /**
         * 客户端认证方式取值列表
         */
        public List<String> clientAuthenticationMethods;

        /**
         * 授权类型取值列表
         */
        public List<String> authorizationGrantTypes;

        /**
         * 重定向 URI 列表
         */
        public List<String> redirectUris;

        /**
         * 登出重定向 URI 列表
         */
        public List<String> postLogoutRedirectUris;

        /**
         * scope 列表
         */
        public List<String> scopes;

        /**
         * client_settings
         */
        public String clientSettings;

        /**
         * token_settings
         */
        public String tokenSettings;
    }
}
