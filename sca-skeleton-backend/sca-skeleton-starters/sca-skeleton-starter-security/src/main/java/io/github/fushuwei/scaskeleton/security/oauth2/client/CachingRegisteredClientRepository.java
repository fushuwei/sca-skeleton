package io.github.fushuwei.scaskeleton.security.oauth2.client;

import io.github.fushuwei.scaskeleton.security.oauth2.authorization.OAuth2AuthorizationJsonMapperFactory;
import io.github.fushuwei.scaskeleton.security.oauth2.authorization.OAuth2AuthorizationRedisKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.Assert;

/**
 * 装饰 JDBC {@link RegisteredClientRepository}：读写时同步将客户端快照写入 Redis，供资源服务器只读加载。
 * <p>
 * 仅在认证中心进程注册；业务微服务使用 {@link RedisRegisteredClientRepository}。
 *
 * @author Fu Wei
 */
@Slf4j
public class CachingRegisteredClientRepository implements RegisteredClientRepository {

    /**
     * 委托的 JDBC 或其它权威数据源。
     */
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
     * @param delegate            权威客户端仓库（通常为 {@code JdbcRegisteredClientRepository}）
     * @param stringRedisTemplate 与授权记录共用的 Redis
     */
    public CachingRegisteredClientRepository(RegisteredClientRepository delegate,
            StringRedisTemplate stringRedisTemplate) {
        Assert.notNull(delegate, "delegate cannot be null");
        Assert.notNull(stringRedisTemplate, "stringRedisTemplate cannot be null");
        this.delegate = delegate;
        this.stringRedisTemplate = stringRedisTemplate;
        // 快照编解码与 RedisRegisteredClientRepository 共用同一格式
        this.redisSerializer = new RegisteredClientRedisSerializer(getClass().getClassLoader());
    }

    /**
     * 持久化到 JDBC 并刷新 Redis 缓存。
     *
     * @param registeredClient 客户端
     */
    @Override
    public void save(RegisteredClient registeredClient) {
        // 权威数据写入 JDBC
        this.delegate.save(registeredClient);
        // 同步刷新 Redis 快照
        cache(registeredClient);
    }

    /**
     * 优先读 Redis，未命中再读 JDBC 并回填缓存。
     *
     * @param id 客户端主键
     * @return 客户端；不存在时返回 null
     */
    @Nullable
    @Override
    public RegisteredClient findById(String id) {
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

    /**
     * 优先读 Redis，未命中再读 JDBC 并回填缓存。
     *
     * @param clientId OAuth2 client_id
     * @return 客户端；不存在时返回 null
     */
    @Nullable
    @Override
    public RegisteredClient findByClientId(String clientId) {
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
     * 反序列化 Redis 中的客户端快照。
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
}
