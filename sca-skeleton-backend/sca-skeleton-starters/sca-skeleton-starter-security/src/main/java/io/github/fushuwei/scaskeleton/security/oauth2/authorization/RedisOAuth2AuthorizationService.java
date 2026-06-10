package io.github.fushuwei.scaskeleton.security.oauth2.authorization;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2DeviceCode;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2UserCode;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的 {@link OAuth2AuthorizationService} 实现。
 * <p>
 * 将 {@link OAuth2Authorization} 以 Hash 结构持久化，字段名与 SAS JDBC 表 {@code oauth2_authorization} 列名一致，
 * 序列化方式与 SAS JDBC 参数映射语义对齐（见 {@link RedisOAuth2AuthorizationParametersMapper}）。
 * 另维护 access / refresh / code / state 等令牌值到授权主键的二级索引，
 * 以支持 {@link #findByToken(String, OAuth2TokenType)} 高效查询。
 * <p>
 * 说明：启用本实现后，MySQL 中的 {@code oauth2_authorization} 表不再写入；注册客户端与 consent 仍建议保留在 JDBC。
 *
 * @author Fu Wei
 */
public class RedisOAuth2AuthorizationService implements OAuth2AuthorizationService {

    /**
     * 与 {@link RedisOAuth2AuthorizationParametersMapper#apply} 返回顺序严格一致的列名，
     * 用于将 SQL 参数列表映射为 Redis Hash 的 field。
     */
    private static final String[] AUTHORIZATION_HASH_FIELDS = new String[] {
            "id",
            "registered_client_id",
            "principal_name",
            "authorization_grant_type",
            "authorized_scopes",
            "attributes",
            "state",
            "authorization_code_value",
            "authorization_code_issued_at",
            "authorization_code_expires_at",
            "authorization_code_metadata",
            "access_token_value",
            "access_token_issued_at",
            "access_token_expires_at",
            "access_token_metadata",
            "access_token_type",
            "access_token_scopes",
            "oidc_id_token_value",
            "oidc_id_token_issued_at",
            "oidc_id_token_expires_at",
            "oidc_id_token_metadata",
            "refresh_token_value",
            "refresh_token_issued_at",
            "refresh_token_expires_at",
            "refresh_token_metadata",
            "user_code_value",
            "user_code_issued_at",
            "user_code_expires_at",
            "user_code_metadata",
            "device_code_value",
            "device_code_issued_at",
            "device_code_expires_at",
            "device_code_metadata"
    };

    /**
     * 用于反序列化 attributes、各 token metadata 等 JSON 字段的 JsonMapper，模块与 SAS JDBC 默认实现一致。
     */
    private final JsonMapper authorizationJsonMapper;

    /**
     * 注册客户端仓库：从 Redis 读出 registered_client_id 后加载完整 {@link RegisteredClient} 以重建授权对象。
     */
    private final RegisteredClientRepository registeredClientRepository;

    /**
     * Spring Data Redis 字符串模板：读写 Hash 与索引键。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 将 {@link OAuth2Authorization} 转为与 JDBC 插入语句相同顺序的 SQL 参数列表（官方实现）。
     */
    private final RedisOAuth2AuthorizationParametersMapper parametersMapper;

    /**
     * 构造 Redis 授权存储：初始化 JsonMapper、参数映射器与 Redis 访问。
     *
     * @param registeredClientRepository 注册客户端仓库，不可为 null
     * @param stringRedisTemplate        字符串 Redis 模板，不可为 null
     */
    public RedisOAuth2AuthorizationService(RegisteredClientRepository registeredClientRepository,
            StringRedisTemplate stringRedisTemplate) {
        Assert.notNull(registeredClientRepository, "registeredClientRepository cannot be null");
        Assert.notNull(stringRedisTemplate, "stringRedisTemplate cannot be null");
        this.registeredClientRepository = registeredClientRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        // 创建与 SAS JDBC 对齐的 JsonMapper，用于 attributes / metadata 等 JSON 字段
        this.authorizationJsonMapper = OAuth2AuthorizationJsonMapperFactory.create(getClass().getClassLoader());
        // 独立参数映射器，避免依赖 JdbcOAuth2AuthorizationService 静态 columnMetadataMap
        this.parametersMapper = new RedisOAuth2AuthorizationParametersMapper(this.authorizationJsonMapper);
    }

    /**
     * 保存或覆盖授权记录：先移除旧索引，再写入 Hash 并建立新索引与 TTL。
     *
     * @param authorization 待持久化的授权对象，不可为 null
     */
    @Override
    public void save(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");
        // 若已存在同 id 授权，先清理旧令牌索引，避免刷新令牌后旧 access 索引残留
        OAuth2Authorization existing = findById(authorization.getId());
        if (existing != null) {
            removeIndexes(existing);
        }
        // 将授权对象转为与 JDBC 相同顺序的 SQL 参数列表
        List<SqlParameterValue> parameters = this.parametersMapper.apply(authorization);
        if (parameters.size() != AUTHORIZATION_HASH_FIELDS.length) {
            throw new IllegalStateException(
                    "OAuth2Authorization SQL parameter count mismatch: expected " + AUTHORIZATION_HASH_FIELDS.length
                            + ", actual " + parameters.size());
        }
        // 按列名映射写入 Redis Hash
        String key = authKey(authorization.getId());
        Map<String, String> hash = new LinkedHashMap<>();
        for (int i = 0; i < AUTHORIZATION_HASH_FIELDS.length; i++) {
            hash.put(AUTHORIZATION_HASH_FIELDS[i], sqlParameterToRedisString(parameters.get(i)));
        }
        this.stringRedisTemplate.opsForHash().putAll(key, hash);
        // 根据 access / refresh 过期时间设置 Hash TTL
        long ttlSeconds = computeAuthorizationTtlSeconds(authorization);
        if (ttlSeconds > 0) {
            this.stringRedisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
        }
        // 建立各类令牌值到授权 id 的二级索引
        addIndexes(authorization, ttlSeconds);
    }

    /**
     * 删除授权：移除主 Hash 与全部相关令牌索引。
     *
     * @param authorization 待删除的授权对象，不可为 null
     */
    @Override
    public void remove(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");
        // 先删令牌索引，再删授权主 Hash
        removeIndexes(authorization);
        this.stringRedisTemplate.delete(authKey(authorization.getId()));
    }

    /**
     * 按主键 id 查询授权。
     *
     * @param id 授权主键，不可为空
     * @return 授权对象；不存在时返回 null
     */
    @Nullable
    @Override
    public OAuth2Authorization findById(String id) {
        Assert.hasText(id, "id cannot be empty");
        Map<Object, Object> entries = this.stringRedisTemplate.opsForHash().entries(authKey(id));
        // Hash 不存在或已过期
        if (entries.isEmpty()) {
            return null;
        }
        // 将 Hash 还原为 OAuth2Authorization
        return mapHashToAuthorization(stringMap(entries));
    }

    /**
     * 按令牌值与可选类型查询授权；类型为 null 时按 SAS JDBC 相同优先级尝试多种令牌。
     *
     * @param token     令牌明文，不可为空
     * @param tokenType 令牌类型，可为 null 表示未知
     * @return 授权对象；不存在时返回 null
     */
    @Nullable
    @Override
    public OAuth2Authorization findByToken(String token, @Nullable OAuth2TokenType tokenType) {
        Assert.hasText(token, "token cannot be empty");
        // 未指定令牌类型时，按 SAS JDBC 相同优先级依次尝试
        if (tokenType == null) {
            String id = this.stringRedisTemplate.opsForValue().get(idxKey("state", token));
            if (id != null) {
                return findById(id);
            }
            id = this.stringRedisTemplate.opsForValue().get(idxKey("code", token));
            if (id != null) {
                return findById(id);
            }
            id = this.stringRedisTemplate.opsForValue().get(idxKey("access", token));
            if (id != null) {
                return findById(id);
            }
            id = this.stringRedisTemplate.opsForValue().get(idxKey("id_token", token));
            if (id != null) {
                return findById(id);
            }
            id = this.stringRedisTemplate.opsForValue().get(idxKey("refresh", token));
            if (id != null) {
                return findById(id);
            }
            id = this.stringRedisTemplate.opsForValue().get(idxKey("user_code", token));
            if (id != null) {
                return findById(id);
            }
            id = this.stringRedisTemplate.opsForValue().get(idxKey("device_code", token));
            return id != null ? findById(id) : null;
        }
        // 已知令牌类型时，直接查对应索引
        if (OAuth2ParameterNames.STATE.equals(tokenType.getValue())) {
            String id = this.stringRedisTemplate.opsForValue().get(idxKey("state", token));
            return id != null ? findById(id) : null;
        }
        if (OAuth2ParameterNames.CODE.equals(tokenType.getValue())) {
            String id = this.stringRedisTemplate.opsForValue().get(idxKey("code", token));
            return id != null ? findById(id) : null;
        }
        if (OAuth2TokenType.ACCESS_TOKEN.equals(tokenType)) {
            String id = this.stringRedisTemplate.opsForValue().get(idxKey("access", token));
            return id != null ? findById(id) : null;
        }
        if (OidcParameterNames.ID_TOKEN.equals(tokenType.getValue())) {
            String id = this.stringRedisTemplate.opsForValue().get(idxKey("id_token", token));
            return id != null ? findById(id) : null;
        }
        if (OAuth2TokenType.REFRESH_TOKEN.equals(tokenType)) {
            String id = this.stringRedisTemplate.opsForValue().get(idxKey("refresh", token));
            return id != null ? findById(id) : null;
        }
        if (OAuth2ParameterNames.USER_CODE.equals(tokenType.getValue())) {
            String id = this.stringRedisTemplate.opsForValue().get(idxKey("user_code", token));
            return id != null ? findById(id) : null;
        }
        if (OAuth2ParameterNames.DEVICE_CODE.equals(tokenType.getValue())) {
            String id = this.stringRedisTemplate.opsForValue().get(idxKey("device_code", token));
            return id != null ? findById(id) : null;
        }
        // 不支持的令牌类型
        return null;
    }

    /**
     * 将 Redis Hash 的 Object 键值统一转为 String Map，便于按列名读取。
     *
     * @param raw Redis 返回的 entries
     * @return 全部为 String 的 Map
     */
    private static Map<String, String> stringMap(Map<Object, Object> raw) {
        Map<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> e : raw.entrySet()) {
            map.put(Objects.toString(e.getKey(), ""), e.getValue() != null ? e.getValue().toString() : "");
        }
        return map;
    }

    /**
     * 授权主键对应的 Redis Hash 键名。
     *
     * @param id 授权 id
     * @return 完整 Redis key
     */
    private static String authKey(String id) {
        return OAuth2AuthorizationRedisKeys.authorizationKey(id);
    }

    /**
     * 令牌索引键：类型 + 令牌值唯一定位一条授权 id。
     *
     * @param type  索引类型（state、access、refresh 等）
     * @param token 令牌明文
     * @return 索引 Redis key
     */
    private static String idxKey(String type, String token) {
        return OAuth2AuthorizationRedisKeys.IDX_PREFIX + type + ":" + token;
    }

    /**
     * 将 {@link SqlParameterValue} 转为写入 Redis Hash 的字符串：时间戳用 epoch 毫秒，二进制用 UTF-8 文本。
     *
     * @param v SQL 参数值
     * @return 非 null 的字符串（空值用空串表示）
     */
    private static String sqlParameterToRedisString(SqlParameterValue v) {
        if (v == null || v.getValue() == null) {
            return "";
        }
        Object val = v.getValue();
        // JDBC Timestamp 存 epoch 毫秒
        if (val instanceof Timestamp ts) {
            return Long.toString(ts.toInstant().toEpochMilli());
        }
        // 二进制列按 UTF-8 文本写入
        if (val instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return val.toString();
    }

    /**
     * 计算授权 Hash 的 TTL 秒数：取 access 与 refresh 过期时间中较晚者与当前时间的差，至少 1 秒。
     *
     * @param authorization 授权对象
     * @return TTL 秒数；无法计算时返回 0 表示不设置过期
     */
    private static long computeAuthorizationTtlSeconds(OAuth2Authorization authorization) {
        Instant latest = null;
        OAuth2Authorization.Token<OAuth2AccessToken> at = authorization.getAccessToken();
        if (at != null && at.getToken().getExpiresAt() != null) {
            latest = at.getToken().getExpiresAt();
        }
        OAuth2Authorization.Token<OAuth2RefreshToken> rt = authorization.getRefreshToken();
        // 取 access 与 refresh 中较晚的过期时刻
        if (rt != null && rt.getToken().getExpiresAt() != null) {
            Instant exp = rt.getToken().getExpiresAt();
            latest = latest == null || exp.isAfter(latest) ? exp : latest;
        }
        if (latest == null) {
            return 0;
        }
        long seconds = Duration.between(Instant.now(), latest).getSeconds();
        // 至少保留 1 秒，避免 TTL 为 0 导致键永不过期
        return Math.max(1, seconds);
    }

    /**
     * 为授权中的各类令牌写入索引键，并在可能时设置与令牌生命周期一致的 TTL。
     *
     * @param authorization 授权对象
     * @param fallbackTtl   当某令牌无过期时间时使用的回退 TTL（秒）
     */
    private void addIndexes(OAuth2Authorization authorization, long fallbackTtl) {
        String id = authorization.getId();
        String state = authorization.getAttribute(OAuth2ParameterNames.STATE);
        if (StringUtils.hasText(state)) {
            setIndex(idxKey("state", state), id, fallbackTtl);
        }
        OAuth2Authorization.Token<OAuth2AuthorizationCode> code = authorization.getToken(OAuth2AuthorizationCode.class);
        if (code != null) {
            long ttl = tokenTtlSeconds(code.getToken().getExpiresAt(), fallbackTtl);
            setIndex(idxKey("code", code.getToken().getTokenValue()), id, ttl);
        }
        OAuth2Authorization.Token<OAuth2AccessToken> access = authorization.getAccessToken();
        if (access != null) {
            long ttl = tokenTtlSeconds(access.getToken().getExpiresAt(), fallbackTtl);
            // access_token 索引供资源服务器 findByToken 自省
            setIndex(idxKey("access", access.getToken().getTokenValue()), id, ttl);
        }
        OAuth2Authorization.Token<OidcIdToken> idToken = authorization.getToken(OidcIdToken.class);
        if (idToken != null) {
            long ttl = tokenTtlSeconds(idToken.getToken().getExpiresAt(), fallbackTtl);
            setIndex(idxKey("id_token", idToken.getToken().getTokenValue()), id, ttl);
        }
        OAuth2Authorization.Token<OAuth2RefreshToken> refresh = authorization.getRefreshToken();
        if (refresh != null) {
            long ttl = tokenTtlSeconds(refresh.getToken().getExpiresAt(), fallbackTtl);
            setIndex(idxKey("refresh", refresh.getToken().getTokenValue()), id, ttl);
        }
        OAuth2Authorization.Token<OAuth2UserCode> userCode = authorization.getToken(OAuth2UserCode.class);
        if (userCode != null) {
            long ttl = tokenTtlSeconds(userCode.getToken().getExpiresAt(), fallbackTtl);
            setIndex(idxKey("user_code", userCode.getToken().getTokenValue()), id, ttl);
        }
        OAuth2Authorization.Token<OAuth2DeviceCode> deviceCode = authorization.getToken(OAuth2DeviceCode.class);
        if (deviceCode != null) {
            long ttl = tokenTtlSeconds(deviceCode.getToken().getExpiresAt(), fallbackTtl);
            setIndex(idxKey("device_code", deviceCode.getToken().getTokenValue()), id, ttl);
        }
    }

    /**
     * 删除与授权关联的全部索引键（按令牌值定位的那些键）。
     *
     * @param authorization 授权对象
     */
    private void removeIndexes(OAuth2Authorization authorization) {
        String state = authorization.getAttribute(OAuth2ParameterNames.STATE);
        if (StringUtils.hasText(state)) {
            this.stringRedisTemplate.delete(idxKey("state", state));
        }
        OAuth2Authorization.Token<OAuth2AuthorizationCode> code = authorization.getToken(OAuth2AuthorizationCode.class);
        if (code != null) {
            this.stringRedisTemplate.delete(idxKey("code", code.getToken().getTokenValue()));
        }
        OAuth2Authorization.Token<OAuth2AccessToken> access = authorization.getAccessToken();
        if (access != null) {
            this.stringRedisTemplate.delete(idxKey("access", access.getToken().getTokenValue()));
        }
        OAuth2Authorization.Token<OidcIdToken> idToken = authorization.getToken(OidcIdToken.class);
        if (idToken != null) {
            this.stringRedisTemplate.delete(idxKey("id_token", idToken.getToken().getTokenValue()));
        }
        OAuth2Authorization.Token<OAuth2RefreshToken> refresh = authorization.getRefreshToken();
        if (refresh != null) {
            this.stringRedisTemplate.delete(idxKey("refresh", refresh.getToken().getTokenValue()));
        }
        OAuth2Authorization.Token<OAuth2UserCode> userCode = authorization.getToken(OAuth2UserCode.class);
        if (userCode != null) {
            this.stringRedisTemplate.delete(idxKey("user_code", userCode.getToken().getTokenValue()));
        }
        OAuth2Authorization.Token<OAuth2DeviceCode> deviceCode = authorization.getToken(OAuth2DeviceCode.class);
        if (deviceCode != null) {
            this.stringRedisTemplate.delete(idxKey("device_code", deviceCode.getToken().getTokenValue()));
        }
    }

    /**
     * 写入索引值并设置 TTL；ttlSeconds &lt;= 0 时不设置过期时间。
     *
     * @param redisKey    索引键
     * @param id          授权主键 id
     * @param ttlSeconds  过期秒数
     */
    private void setIndex(String redisKey, String id, long ttlSeconds) {
        // 原子写入 value + TTL，避免 set 与 expire 分离导致索引永久驻留
        if (ttlSeconds > 0) {
            this.stringRedisTemplate.opsForValue().set(redisKey, id, ttlSeconds, TimeUnit.SECONDS);
            return;
        }
        this.stringRedisTemplate.opsForValue().set(redisKey, id);
    }

    /**
     * 根据令牌过期时间计算索引 TTL；无过期时间则使用回退值。
     *
     * @param expiresAt   令牌过期时刻，可为 null
     * @param fallbackTtl 回退 TTL（秒）
     * @return TTL 秒数
     */
    private static long tokenTtlSeconds(@Nullable Instant expiresAt, long fallbackTtl) {
        if (expiresAt == null) {
            return fallbackTtl;
        }
        long seconds = Duration.between(Instant.now(), expiresAt).getSeconds();
        return Math.max(1, seconds);
    }

    /**
     * 将列 Map 还原为 {@link OAuth2Authorization}，逻辑对齐 SAS {@code AbstractOAuth2AuthorizationRowMapper#mapRow}。
     *
     * @param cols 列名到字符串值的映射
     * @return 构建完成的授权对象
     */
    @SuppressWarnings("unchecked")
    private OAuth2Authorization mapHashToAuthorization(Map<String, String> cols) {
        // 加载注册客户端，缺失则无法重建授权
        String registeredClientId = cols.get("registered_client_id");
        RegisteredClient registeredClient = this.registeredClientRepository.findById(registeredClientId);
        if (registeredClient == null) {
            throw new DataRetrievalFailureException(
                    "The RegisteredClient with id '" + registeredClientId + "' was not found in the RegisteredClientRepository.");
        }
        OAuth2Authorization.Builder builder = OAuth2Authorization.withRegisteredClient(registeredClient);
        String id = cols.get("id");
        String principalName = cols.get("principal_name");
        String authorizationGrantType = cols.get("authorization_grant_type");
        Set<String> authorizedScopes = Collections.emptySet();
        String authorizedScopesString = emptyToNull(cols.get("authorized_scopes"));
        if (authorizedScopesString != null) {
            authorizedScopes = StringUtils.commaDelimitedListToSet(authorizedScopesString);
        }
        Map<String, Object> attributes = parseJsonMap(cols.get("attributes"));
        builder.id(id)
                .principalName(principalName)
                .authorizationGrantType(new AuthorizationGrantType(authorizationGrantType))
                .authorizedScopes(authorizedScopes)
                .attributes(attrs -> attrs.putAll(attributes));
        // 可选 state 属性
        String state = emptyToNull(cols.get("state"));
        if (StringUtils.hasText(state)) {
            builder.attribute(OAuth2ParameterNames.STATE, state);
        }
        Instant tokenIssuedAt;
        Instant tokenExpiresAt;
        // 授权码
        String authorizationCodeValue = emptyToNull(cols.get("authorization_code_value"));
        if (StringUtils.hasText(authorizationCodeValue)) {
            tokenIssuedAt = parseInstantMillis(cols.get("authorization_code_issued_at"));
            tokenExpiresAt = parseInstantMillis(cols.get("authorization_code_expires_at"));
            Map<String, Object> authorizationCodeMetadata = parseJsonMap(cols.get("authorization_code_metadata"));
            OAuth2AuthorizationCode authorizationCode =
                    new OAuth2AuthorizationCode(authorizationCodeValue, tokenIssuedAt, tokenExpiresAt);
            builder.token(authorizationCode, metadata -> metadata.putAll(authorizationCodeMetadata));
        }
        // access_token
        String accessTokenValue = emptyToNull(cols.get("access_token_value"));
        if (StringUtils.hasText(accessTokenValue)) {
            tokenIssuedAt = parseInstantMillis(cols.get("access_token_issued_at"));
            tokenExpiresAt = parseInstantMillis(cols.get("access_token_expires_at"));
            Map<String, Object> accessTokenMetadata = parseJsonMap(cols.get("access_token_metadata"));
            OAuth2AccessToken.TokenType tokenType = null;
            String accessTokenType = emptyToNull(cols.get("access_token_type"));
            if (OAuth2AccessToken.TokenType.BEARER.getValue().equalsIgnoreCase(accessTokenType)) {
                tokenType = OAuth2AccessToken.TokenType.BEARER;
            } else if (OAuth2AccessToken.TokenType.DPOP.getValue().equalsIgnoreCase(accessTokenType)) {
                tokenType = OAuth2AccessToken.TokenType.DPOP;
            }
            Set<String> scopes = Collections.emptySet();
            String accessTokenScopes = emptyToNull(cols.get("access_token_scopes"));
            if (accessTokenScopes != null) {
                scopes = StringUtils.commaDelimitedListToSet(accessTokenScopes);
            }
            OAuth2AccessToken accessToken = new OAuth2AccessToken(tokenType, accessTokenValue, tokenIssuedAt,
                    tokenExpiresAt, scopes);
            builder.token(accessToken, metadata -> metadata.putAll(accessTokenMetadata));
        }
        // OIDC id_token
        String oidcIdTokenValue = emptyToNull(cols.get("oidc_id_token_value"));
        if (StringUtils.hasText(oidcIdTokenValue)) {
            tokenIssuedAt = parseInstantMillis(cols.get("oidc_id_token_issued_at"));
            tokenExpiresAt = parseInstantMillis(cols.get("oidc_id_token_expires_at"));
            Map<String, Object> oidcTokenMetadata = parseJsonMap(cols.get("oidc_id_token_metadata"));
            Object idClaimsObj = oidcTokenMetadata.get(OAuth2Authorization.Token.CLAIMS_METADATA_NAME);
            Map<String, Object> idClaims = idClaimsObj instanceof Map<?, ?> m
                    ? (Map<String, Object>) m
                    : Collections.emptyMap();
            OidcIdToken oidcToken = new OidcIdToken(oidcIdTokenValue, tokenIssuedAt, tokenExpiresAt, idClaims);
            builder.token(oidcToken, metadata -> metadata.putAll(oidcTokenMetadata));
        }
        // refresh_token
        String refreshTokenValue = emptyToNull(cols.get("refresh_token_value"));
        if (StringUtils.hasText(refreshTokenValue)) {
            tokenIssuedAt = parseInstantMillis(cols.get("refresh_token_issued_at"));
            tokenExpiresAt = null;
            String refreshExpires = emptyToNull(cols.get("refresh_token_expires_at"));
            if (refreshExpires != null) {
                tokenExpiresAt = parseInstantMillis(refreshExpires);
            }
            Map<String, Object> refreshTokenMetadata = parseJsonMap(cols.get("refresh_token_metadata"));
            OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(refreshTokenValue, tokenIssuedAt, tokenExpiresAt);
            builder.token(refreshToken, metadata -> metadata.putAll(refreshTokenMetadata));
        }
        // user_code（设备流）
        String userCodeValue = emptyToNull(cols.get("user_code_value"));
        if (StringUtils.hasText(userCodeValue)) {
            tokenIssuedAt = parseInstantMillis(cols.get("user_code_issued_at"));
            tokenExpiresAt = parseInstantMillis(cols.get("user_code_expires_at"));
            Map<String, Object> userCodeMetadata = parseJsonMap(cols.get("user_code_metadata"));
            OAuth2UserCode userCode = new OAuth2UserCode(userCodeValue, tokenIssuedAt, tokenExpiresAt);
            builder.token(userCode, metadata -> metadata.putAll(userCodeMetadata));
        }
        // device_code（设备流）
        String deviceCodeValue = emptyToNull(cols.get("device_code_value"));
        if (StringUtils.hasText(deviceCodeValue)) {
            tokenIssuedAt = parseInstantMillis(cols.get("device_code_issued_at"));
            tokenExpiresAt = parseInstantMillis(cols.get("device_code_expires_at"));
            Map<String, Object> deviceCodeMetadata = parseJsonMap(cols.get("device_code_metadata"));
            OAuth2DeviceCode deviceCode = new OAuth2DeviceCode(deviceCodeValue, tokenIssuedAt, tokenExpiresAt);
            builder.token(deviceCode, metadata -> metadata.putAll(deviceCodeMetadata));
        }
        return builder.build();
    }

    /**
     * 空串视为 null，便于统一判断“无值”。
     *
     * @param s 原始字符串
     * @return 去空后的值或 null
     */
    @Nullable
    private static String emptyToNull(String s) {
        return StringUtils.hasText(s) ? s : null;
    }

    /**
     * 将 epoch 毫秒字符串解析为 {@link Instant}；无效或空时返回 null。
     *
     * @param millis 毫秒数字字符串
     * @return 时刻或 null
     */
    @Nullable
    private static Instant parseInstantMillis(String millis) {
        if (!StringUtils.hasText(millis)) {
            return null;
        }
        try {
            return Instant.ofEpochMilli(Long.parseLong(millis.trim()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 使用与 SAS JDBC 相同的 JsonMapper 将 JSON 对象反序列化为 Map。
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
            // 反序列化为 Map，供 attributes / metadata 使用
            return this.authorizationJsonMapper.readValue(json, javaType);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse OAuth2 JSON map: " + ex.getMessage(), ex);
        }
    }
}
