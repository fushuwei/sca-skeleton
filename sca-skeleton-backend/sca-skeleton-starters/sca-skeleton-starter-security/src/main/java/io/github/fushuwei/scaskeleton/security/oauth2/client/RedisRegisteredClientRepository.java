package io.github.fushuwei.scaskeleton.security.oauth2.client;

import io.github.fushuwei.scaskeleton.security.oauth2.authorization.OAuth2AuthorizationRedisKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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
 *       <b>「授权服务器」使用场景：</b>（提供 delegate）— JDBC 为主存储，Redis 为缓存：
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
     * 授权服务器使用场景构造器
     *
     * @param delegate            注册客户端存储库（通常为 {@code JdbcRegisteredClientRepository}）
     * @param stringRedisTemplate Redis 字符串模板
     * @param securityJsonMapper  OAuth2 持久层专用 JsonMapper
     */
    public RedisRegisteredClientRepository(RegisteredClientRepository delegate,
                                           StringRedisTemplate stringRedisTemplate,
                                           @Qualifier("securityJsonMapper") JsonMapper securityJsonMapper) {
        this.delegate = delegate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisSerializer = new RegisteredClientRedisSerializer(securityJsonMapper);
    }

    /**
     * 资源服务器使用场景构造器
     *
     * @param stringRedisTemplate Redis 字符串模板
     * @param securityJsonMapper  OAuth2 持久层专用 JsonMapper
     */
    public RedisRegisteredClientRepository(StringRedisTemplate stringRedisTemplate,
                                           @Qualifier("securityJsonMapper") JsonMapper securityJsonMapper) {
        this(null, stringRedisTemplate, securityJsonMapper);
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
        // 先查询 Redis 缓存
        String cacheKey = OAuth2AuthorizationRedisKeys.clientKey(id);
        String json = this.stringRedisTemplate.opsForValue().get(cacheKey);
        RegisteredClient client = deserialize(json);
        if (client != null) {
            return client;
        }

        // 缓存未命中或反序列化异常等情况，从 Redis 中删除当前无效缓存
        if (StringUtils.hasText(json)) {
            log.warn("RegisteredClient 缓存无效或已过期，key={}", cacheKey);
            this.stringRedisTemplate.delete(cacheKey);
        }

        // 授权服务器：查询 JDBC 并回填 Redis 缓存
        if (this.delegate != null) {
            client = this.delegate.findById(id);
            if (client != null) {
                cache(client);
            }
            return client;
        }

        // 资源服务器：Redis 是唯一数据源，缓存查不到或反序列化异常就直接报错
        throw new DataRetrievalFailureException("Redis 缓存中未找到有效的 RegisteredClient，id: " + id);
    }

    /**
     * 根据 client_id 查询注册客户端
     *
     * @param clientId 客户端 ID
     * @return 注册客户端
     */
    @Override
    public RegisteredClient findByClientId(String clientId) {
        // 通过 client_id 找到主键 id
        String cacheKey = OAuth2AuthorizationRedisKeys.clientIdIndexKey(clientId);
        String id = this.stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(id)) {
            RegisteredClient client = findById(id);
            if (client != null) {
                return client;
            }
        }

        // 缓存已失效
        this.stringRedisTemplate.delete(cacheKey);

        // 授权服务器
        if (this.delegate != null) {
            // 查询 JDBC 并回填 Redis 缓存
            RegisteredClient client = this.delegate.findByClientId(clientId);
            if (client != null) {
                cache(client);
            }
            return client;
        }

        // 资源服务器
        return null;
    }

    /**
     * 将注册客户端写入 Redis 缓存
     *
     * @param registeredClient 注册客户端
     */
    private void cache(RegisteredClient registeredClient) {
        try {
            String json = this.redisSerializer.serialize(registeredClient);
            this.stringRedisTemplate.opsForValue()
                .set(OAuth2AuthorizationRedisKeys.clientKey(registeredClient.getId()), json);
            this.stringRedisTemplate.opsForValue()
                .set(OAuth2AuthorizationRedisKeys.clientIdIndexKey(registeredClient.getClientId()),
                    registeredClient.getId());
        } catch (Exception e) {
            throw new IllegalStateException("Redis 缓存 RegisteredClient 异常: " + e.getMessage(), e);
        }
    }

    /**
     * 将 Redis 中的 JSON 字符串反序列化成 RegisteredClient
     *
     * @param json JSON 字符串
     * @return 注册客户端
     */
    private RegisteredClient deserialize(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return this.redisSerializer.deserialize(json);
        } catch (Exception e) {
            log.warn("RegisteredClient 反序列化异常", e);
            return null;
        }
    }
}
