package io.github.fushuwei.scaskeleton.security.oauth2.client;

import io.github.fushuwei.scaskeleton.security.oauth2.authorization.OAuth2AuthorizationRedisKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * 基于 Redis 的 {@link RegisteredClientRepository} 实现
 * <p>
 * 提供两种使用场景：
 * <ol>
 *   <li>
 *       <b>「认证中心」使用场景：</b>（提供 delegate）— JDBC 为主存储，Redis 为缓存：
 *       {@code save} 写入 JDBC + 同步刷新 Redis 快照，{@code find} 优先读 Redis 未命中则回源 JDBC 并回填
 *   </li>
 *   <li>
 *       <b>「资源服务器」使用场景：</b>（不提供 delegate）— Redis 只读加载，写入抛异常
 *   </li>
 * </ol>
 *
 * @author Fu Wei
 */
@Slf4j
public class RedisRegisteredClientRepository implements RegisteredClientRepository {

    /**
     * 委托的 JDBC 注册客户端存储库
     */
    private final RegisteredClientRepository delegate;

    /**
     * Redis 字符串模板
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 注册客户端 Redis 存储库数据序列化器
     */
    private final RegisteredClientRedisSerializer redisSerializer;

    /**
     * 认证中心使用场景构造器
     *
     * @param delegate                注册客户端存储库（通常为 {@code JdbcRegisteredClientRepository}）
     * @param stringRedisTemplate     与授权记录共用的 Redis
     * @param authorizationJsonMapper OAuth2 持久层专用 JsonMapper
     */
    public RedisRegisteredClientRepository(RegisteredClientRepository delegate,
                                           StringRedisTemplate stringRedisTemplate,
                                           JsonMapper authorizationJsonMapper) {
        this.delegate = delegate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisSerializer = new RegisteredClientRedisSerializer(authorizationJsonMapper);
    }

    /**
     * 资源服务器使用场景构造器
     *
     * @param stringRedisTemplate     Redis 字符串模板
     * @param authorizationJsonMapper OAuth2 持久层专用 JsonMapper
     */
    public RedisRegisteredClientRepository(StringRedisTemplate stringRedisTemplate,
                                           JsonMapper authorizationJsonMapper) {
        this(null, stringRedisTemplate, authorizationJsonMapper);
    }

    /**
     * 持久化注册客户端
     *
     * @param registeredClient 客户端
     */
    @Override
    public void save(RegisteredClient registeredClient) {
        if (this.delegate != null) {
            // JDBC 持久化
            this.delegate.save(registeredClient);
            // 同步刷新 Redis 缓存
            cache(registeredClient);
        }
    }

    /**
     * 根据主键 ID 查询注册客户端
     *
     * @param id 主键 ID
     * @return 注册客户端
     */
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
        String json = this.stringRedisTemplate.opsForValue().get(OAuth2AuthorizationRedisKeys.registeredClientIdKey(id));
        // 从主键键读取快照 JSON 并反序列化
        return deserialize(json);
    }

    /**
     * 根据 client_id 查询注册客户端
     *
     * @param clientId 客户端 ID
     * @return 注册客户端
     */
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
        String id = this.stringRedisTemplate.opsForValue().get(OAuth2AuthorizationRedisKeys.registeredClientClientIdKey(clientId));
        if (!StringUtils.hasText(id)) {
            return null;
        }
        // 通过 client_id 索引定位主键后再加载快照
        return findById(id);
    }

    /**
     * 将注册客户端写入 Redis 缓存
     *
     * @param registeredClient 注册客户端
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
     * 反序列化 Redis 中的注册客户端快照
     *
     * @param json     JSON 字符串
     * @param cacheKey 当前缓存键；解析失败或历史格式时用于失效旧数据
     * @return 客户端；解析失败时失效缓存并返回 null，由调用方回源 JDBC
     */
    private RegisteredClient deserialize(String json, String cacheKey) {
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
     * 将 JSON 反序列化为 {@link RegisteredClient}
     *
     * @param json 缓存 JSON
     * @return 客户端；空输入返回 null
     */
    private RegisteredClient deserialize(String json) {
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
