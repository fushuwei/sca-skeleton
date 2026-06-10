package io.github.fushuwei.scaskeleton.security.oauth2.client;

import io.github.fushuwei.scaskeleton.security.oauth2.authorization.OAuth2AuthorizationJsonMapperFactory;
import io.github.fushuwei.scaskeleton.security.oauth2.authorization.OAuth2AuthorizationRedisKeys;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 基于 Redis 的 {@link RegisteredClientRepository} 实现。
 * <p>
 * 根据不同部署场景提供两种模式：
 * <ol>
 *   <li><b>认证中心模式</b>（提供 delegate）— JDBC 为主存储，Redis 为缓存：
 *       {@code save} 写入 JDBC + 同步刷新 Redis 快照，{@code find} 优先读 Redis 未命中则回源 JDBC 并回填。</li>
 *   <li><b>资源服务器模式</b>（不提供 delegate）— Redis 只读加载，写入抛异常。</li>
 * </ol>
 *
 * @author Fu Wei
 */
@Slf4j
public class RedisRegisteredClientRepository implements RegisteredClientRepository {

    /**
     * 委托的 JDBC 或其它权威数据源（认证中心模式时非 null，资源服务器模式时为 null）。
     */
    @Nullable
    private final RegisteredClientRepository delegate;

    /**
     * Redis 字符串模板。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 注册客户端 Redis 快照编解码器。
     */
    private final RegisteredClientRedisSerializer redisSerializer;

    /**
     * 认证中心模式构造器。
     *
     * @param delegate            权威客户端仓库（通常为 {@code JdbcRegisteredClientRepository}）
     * @param stringRedisTemplate 与授权记录共用的 Redis
     */
    public RedisRegisteredClientRepository(RegisteredClientRepository delegate,
                                           StringRedisTemplate stringRedisTemplate) {
        Assert.notNull(delegate, "delegate cannot be null");
        Assert.notNull(stringRedisTemplate, "stringRedisTemplate cannot be null");
        this.delegate = delegate;
        this.stringRedisTemplate = stringRedisTemplate;
        // 快照编解码与资源服务器共用同一格式
        this.redisSerializer = new RegisteredClientRedisSerializer(getClass().getClassLoader());
    }

    /**
     * 资源服务器模式构造器（只读 Redis）。
     *
     * @param stringRedisTemplate Redis 模板，不可为 null
     */
    public RedisRegisteredClientRepository(StringRedisTemplate stringRedisTemplate) {
        Assert.notNull(stringRedisTemplate, "stringRedisTemplate cannot be null");
        this.delegate = null;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisSerializer = new RegisteredClientRedisSerializer(getClass().getClassLoader());
    }

    /**
     * 持久化客户端。
     * <p>
     * 认证中心模式：写入 JDBC 并同步刷新 Redis 缓存。<br>
     * 资源服务器模式：不支持写入。
     *
     * @param registeredClient 客户端
     */
    @Override
    public void save(RegisteredClient registeredClient) {
        if (this.delegate != null) {
            // 权威数据写入 JDBC
            this.delegate.save(registeredClient);
            // 同步刷新 Redis 快照
            cache(registeredClient);
            return;
        }
        // 资源服务器只读，写入由认证中心完成
        throw new UnsupportedOperationException(
            "RedisRegisteredClientRepository is read-only; register clients on the authorization server.");
    }

    /**
     * 按主键加载注册客户端。
     * <p>
     * 认证中心模式：优先读 Redis，未命中再回源 JDBC 并回填缓存。<br>
     * 资源服务器模式：仅读 Redis。
     *
     * @param id 客户端主键
     * @return 客户端；不存在时返回 null
     */
    @Nullable
    @Override
    public RegisteredClient findById(String id) {
        if (this.delegate != null) {
            String cacheKey = OAuth2AuthorizationRedisKeys.registeredClientIdKey(id);
            String cachedJson = this.stringRedisTemplate.opsForValue().get(cacheKey);
            RegisteredClient cached = deserialize(cachedJson, cacheKey);
            // 缓存命中直接返回
            if (cached != null) {
                return cached;
            }
            // 未命中回源 JDBC 并回填
            RegisteredClient client = this.delegate.findById(id);
            if (client != null) {
                cache(client);
            }
            return client;
        }
        Assert.hasText(id, "id cannot be empty");
        String json = this.stringRedisTemplate.opsForValue().get(OAuth2AuthorizationRedisKeys.registeredClientIdKey(id));
        // 从主键键读取快照 JSON 并反序列化
        return deserialize(json);
    }

    /**
     * 按 OAuth2 client_id 加载注册客户端。
     * <p>
     * 认证中心模式：优先读 Redis 索引，未命中再回源 JDBC 并回填缓存。<br>
     * 资源服务器模式：仅读 Redis 索引。
     *
     * @param clientId client_id
     * @return 客户端；不存在时返回 null
     */
    @Nullable
    @Override
    public RegisteredClient findByClientId(String clientId) {
        if (this.delegate != null) {
            // client_id -> 主键 id 二级索引
            String id = this.stringRedisTemplate.opsForValue()
                .get(OAuth2AuthorizationRedisKeys.registeredClientClientIdKey(clientId));
            if (id != null) {
                RegisteredClient cached = findById(id);
                if (cached != null) {
                    return cached;
                }
                // client_id 索引命中但主键缓存已失效时，清理脏索引避免重复 miss
                this.stringRedisTemplate.delete(OAuth2AuthorizationRedisKeys.registeredClientClientIdKey(clientId));
            }
            // 索引缺失或快照无效时回源 JDBC
            RegisteredClient client = this.delegate.findByClientId(clientId);
            if (client != null) {
                cache(client);
            }
            return client;
        }
        Assert.hasText(clientId, "clientId cannot be empty");
        String id = this.stringRedisTemplate.opsForValue()
            .get(OAuth2AuthorizationRedisKeys.registeredClientClientIdKey(clientId));
        if (!StringUtils.hasText(id)) {
            return null;
        }
        // 通过 client_id 索引定位主键后再加载快照
        return findById(id);
    }

    // ── 认证中心模式专用方法 ──

    /**
     * 将客户端快照写入 Redis（主键与 client_id 双索引）。
     *
     * @param registeredClient 客户端
     */
    private void cache(RegisteredClient registeredClient) {
        try {
            String json = this.redisSerializer.serialize(registeredClient);
            // 主键键存完整快照 JSON
            this.stringRedisTemplate.opsForValue()
                .set(OAuth2AuthorizationRedisKeys.registeredClientIdKey(registeredClient.getId()), json);
            // client_id 键仅存主键 id，便于按 client_id 反查
            this.stringRedisTemplate.opsForValue()
                .set(OAuth2AuthorizationRedisKeys.registeredClientClientIdKey(registeredClient.getClientId()),
                    registeredClient.getId());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to cache RegisteredClient to Redis: " + ex.getMessage(), ex);
        }
    }

    /**
     * 反序列化 Redis 中的客户端快照（认证中心模式，带脏缓存清理）。
     *
     * @param json     JSON 文本
     * @param cacheKey 当前缓存键；解析失败或历史格式时用于失效旧数据
     * @return 客户端；解析失败时失效缓存并返回 null，由调用方回源 JDBC
     */
    @Nullable
    private RegisteredClient deserialize(@Nullable String json, @Nullable String cacheKey) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            RegisteredClient client = this.redisSerializer.deserialize(json);
            // 格式无效时删除脏缓存，由调用方回源 JDBC
            if (client == null && cacheKey != null) {
                log.warn("RegisteredClient Redis 缓存格式无效或已过期，将回源 JDBC 并刷新缓存: key={}", cacheKey);
                this.stringRedisTemplate.delete(cacheKey);
            }
            return client;
        } catch (Exception ex) {
            log.warn("RegisteredClient Redis 缓存反序列化失败，将回源 JDBC 并刷新缓存: key={}", cacheKey, ex);
            if (cacheKey != null) {
                this.stringRedisTemplate.delete(cacheKey);
            }
            return null;
        }
    }

    /**
     * 将 JSON 反序列化为 {@link RegisteredClient}（资源服务器模式，失败抛异常）。
     *
     * @param json 缓存 JSON
     * @return 客户端；空输入返回 null
     */
    @Nullable
    private RegisteredClient deserialize(@Nullable String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            RegisteredClient client = this.redisSerializer.deserialize(json);
            // 快照缺失或格式无效时抛 DataRetrievalFailureException，与 JDBC 语义对齐
            if (client == null) {
                throw new DataRetrievalFailureException(
                    "RegisteredClient Redis cache is missing, invalid, or uses a legacy format");
            }
            return client;
        } catch (DataRetrievalFailureException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DataRetrievalFailureException("Failed to deserialize RegisteredClient from Redis", ex);
        }
    }
}
