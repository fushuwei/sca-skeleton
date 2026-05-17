package io.github.fushuwei.sca.oauth2.redis;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * OAuth2 授权记录在 Redis 中的只读访问器。
 * <p>
 * 供资源服务器 {@link io.github.fushuwei.sca.starter.security.introspection.RedisOpaqueTokenIntrospector}
 * 直接校验不透明 access_token，无需 HTTP 调用 {@code /oauth2/introspect}。
 * <p>
 * 读取路径与认证中心 {@code RedisOAuth2AuthorizationService#findByToken} 对齐：
 * 索引键定位授权 id → 加载 Hash → 校验过期 → 解析 {@code access_token_metadata} 中的 claims。
 *
 * @author Fu Wei
 */
public class OAuth2AuthorizationRedisReader {

    /**
     * 与 SAS JDBC/Redis 实现一致的 JsonMapper，用于反序列化 metadata 中的 JSON Map。
     */
    private final JsonMapper authorizationJsonMapper;

    /**
     * 字符串 Redis 模板，与认证中心共用连接与 database。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * @param stringRedisTemplate Redis 访问模板，不可为 null
     */
    public OAuth2AuthorizationRedisReader(StringRedisTemplate stringRedisTemplate) {
        Assert.notNull(stringRedisTemplate, "stringRedisTemplate cannot be null");
        this.stringRedisTemplate = stringRedisTemplate;
        this.authorizationJsonMapper = JsonMapper.builder()
                .addModules(SecurityJacksonModules.getModules(OAuth2AuthorizationRedisReader.class.getClassLoader()))
                .build();
    }

    /**
     * 按 access_token 明文解析自省 claims。
     * <p>
     * 令牌不存在、已吊销、已过期或 metadata 中无 claims 时返回 {@code null}，
     * 由上层 {@link org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector}
     * 转换为 {@code invalid_token}。
     *
     * @param accessTokenValue Bearer 令牌值（不含 {@code Bearer } 前缀）
     * @return 与 HTTP 自省响应语义一致的 claim Map；无效时返回 null
     */
    @Nullable
    public Map<String, Object> resolveAccessTokenClaims(String accessTokenValue) {
        Assert.hasText(accessTokenValue, "accessTokenValue cannot be empty");
        // 1) 通过 access_token 二级索引定位授权主键
        String authorizationId = this.stringRedisTemplate.opsForValue()
                .get(OAuth2RedisKeys.accessTokenIndexKey(accessTokenValue));
        if (!StringUtils.hasText(authorizationId)) {
            return null;
        }
        // 2) 加载授权 Hash（字段名与 SAS oauth2_authorization 表列名一致）
        Map<Object, Object> rawEntries = this.stringRedisTemplate.opsForHash()
                .entries(OAuth2RedisKeys.authorizationKey(authorizationId));
        if (rawEntries.isEmpty()) {
            return null;
        }
        Map<String, String> cols = toStringMap(rawEntries);
        // 3) 校验 access_token 是否已过期
        if (isAccessTokenExpired(cols.get(OAuth2RedisKeys.FIELD_ACCESS_TOKEN_EXPIRES_AT))) {
            return null;
        }
        // 4) 从 metadata 提取 SAS 在颁发时写入的 claims（ScaOpaqueAccessTokenClaimsCustomizer）
        return extractClaimsFromMetadata(cols.get(OAuth2RedisKeys.FIELD_ACCESS_TOKEN_METADATA));
    }

    /**
     * 将 Redis Hash 的 Object 键值转为 String Map。
     *
     * @param raw Redis entries
     * @return 列名到字符串值的映射
     */
    private static Map<String, String> toStringMap(Map<Object, Object> raw) {
        Map<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> entry : raw.entrySet()) {
            map.put(Objects.toString(entry.getKey(), ""), entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return map;
    }

    /**
     * 判断 access_token 是否已过期。
     *
     * @param expiresAtMillis epoch 毫秒字符串，空表示无法判断（视为未过期，由索引 TTL 兜底）
     * @return true 表示已过期
     */
    private static boolean isAccessTokenExpired(@Nullable String expiresAtMillis) {
        if (!StringUtils.hasText(expiresAtMillis)) {
            return false;
        }
        try {
            Instant expiresAt = Instant.ofEpochMilli(Long.parseLong(expiresAtMillis.trim()));
            return Instant.now().isAfter(expiresAt);
        } catch (NumberFormatException ex) {
            return true;
        }
    }

    /**
     * 从 {@code access_token_metadata} JSON 中解析 claims。
     * <p>
     * SAS 将 {@link org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer}
     * 写入的字段存放在 {@link OAuth2Authorization.Token#CLAIMS_METADATA_NAME} 下。
     *
     * @param metadataJson Redis Hash 中的 metadata 列
     * @return claims Map；解析失败或无 claims 时返回 null
     */
    @Nullable
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractClaimsFromMetadata(@Nullable String metadataJson) {
        if (!StringUtils.hasText(metadataJson)) {
            return null;
        }
        Map<String, Object> metadata = parseJsonMap(metadataJson);
        Object claimsObj = metadata.get(OAuth2Authorization.Token.CLAIMS_METADATA_NAME);
        if (!(claimsObj instanceof Map<?, ?> claimsMap) || claimsMap.isEmpty()) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : claimsMap.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                result.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * 反序列化 JSON 对象为 Map，与认证中心 Redis 实现使用同一 JsonMapper 模块。
     *
     * @param json JSON 文本
     * @return Map；空输入返回空 Map
     */
    private Map<String, Object> parseJsonMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        try {
            ParameterizedTypeReference<Map<String, Object>> typeRef = new ParameterizedTypeReference<>() {
            };
            JavaType javaType = this.authorizationJsonMapper.constructType(typeRef.getType());
            return this.authorizationJsonMapper.readValue(json, javaType);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse OAuth2 metadata JSON: " + ex.getMessage(), ex);
        }
    }
}
