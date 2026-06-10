package io.github.fushuwei.scaskeleton.security.oauth2.client;

import io.github.fushuwei.scaskeleton.security.oauth2.authorization.OAuth2AuthorizationJsonMapperFactory;
import io.github.fushuwei.scaskeleton.security.oauth2.authorization.OAuth2AuthorizationRedisKeys;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 基于 Redis 的 {@link RegisteredClientRepository} 只读实现。
 * <p>
 * 供资源服务器在重建 {@link org.springframework.security.oauth2.server.authorization.OAuth2Authorization} 时加载客户端元数据，
 * 数据由认证中心 {@link CachingRegisteredClientRepository} 在 JDBC 读写时同步写入 Redis。
 *
 * @author Fu Wei
 */
public class RedisRegisteredClientRepository implements RegisteredClientRepository {

    /**
     * 注册客户端 Redis 快照编解码器。
     */
    private final RegisteredClientRedisSerializer redisSerializer;

    /**
     * Redis 字符串模板。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * @param stringRedisTemplate Redis 模板，不可为 null
     */
    public RedisRegisteredClientRepository(StringRedisTemplate stringRedisTemplate) {
        Assert.notNull(stringRedisTemplate, "stringRedisTemplate cannot be null");
        this.stringRedisTemplate = stringRedisTemplate;
        // 与认证中心 CachingRegisteredClientRepository 使用相同快照格式
        this.redisSerializer = new RegisteredClientRedisSerializer(getClass().getClassLoader());
    }

    /**
     * 资源服务器不应通过本实现写入客户端，写入由认证中心 JDBC + 缓存装饰器完成。
     *
     * @param registeredClient 客户端
     */
    @Override
    public void save(RegisteredClient registeredClient) {
        // 资源服务器只读，写入由认证中心完成
        throw new UnsupportedOperationException(
                "RedisRegisteredClientRepository is read-only; register clients on the authorization server.");
    }

    /**
     * 按主键从 Redis 加载注册客户端。
     *
     * @param id 客户端主键
     * @return 客户端；不存在时返回 null
     */
    @Nullable
    @Override
    public RegisteredClient findById(String id) {
        Assert.hasText(id, "id cannot be empty");
        String json = this.stringRedisTemplate.opsForValue().get(OAuth2AuthorizationRedisKeys.registeredClientIdKey(id));
        // 从主键键读取快照 JSON 并反序列化
        return deserialize(json);
    }

    /**
     * 按 OAuth2 client_id 从 Redis 加载注册客户端。
     *
     * @param clientId client_id
     * @return 客户端；不存在时返回 null
     */
    @Nullable
    @Override
    public RegisteredClient findByClientId(String clientId) {
        Assert.hasText(clientId, "clientId cannot be empty");
        String id = this.stringRedisTemplate.opsForValue()
                .get(OAuth2AuthorizationRedisKeys.registeredClientClientIdKey(clientId));
        if (!StringUtils.hasText(id)) {
            return null;
        }
        // 通过 client_id 索引定位主键后再加载快照
        return findById(id);
    }

    /**
     * 将 JSON 反序列化为 {@link RegisteredClient}。
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
