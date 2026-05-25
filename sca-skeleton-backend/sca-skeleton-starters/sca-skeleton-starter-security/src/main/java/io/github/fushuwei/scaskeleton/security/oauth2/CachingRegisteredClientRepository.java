package io.github.fushuwei.scaskeleton.security.oauth2;

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
        this.redisSerializer = new RegisteredClientRedisSerializer(getClass().getClassLoader());
    }

    /**
     * 持久化到 JDBC 并刷新 Redis 缓存。
     *
     * @param registeredClient 客户端
     */
    @Override
    public void save(RegisteredClient registeredClient) {
        this.delegate.save(registeredClient);
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
        if (cached != null) {
            return cached;
        }
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
        String id = this.stringRedisTemplate.opsForValue()
                .get(OAuth2AuthorizationRedisKeys.registeredClientClientIdKey(clientId));
        if (id != null) {
            RegisteredClient cached = findById(id);
            if (cached != null) {
                return cached;
            }
        }
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
            this.stringRedisTemplate.opsForValue()
                    .set(OAuth2AuthorizationRedisKeys.registeredClientIdKey(registeredClient.getId()), json);
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
