package io.github.fushuwei.scaskeleton.core.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET;

/**
 * Redis 自动配置类
 * <p>
 * 启用 Spring 缓存抽象，并配置默认的 {@link RedisTemplate} Bean：
 * Key / HashKey 使用 String 序列化，Value / HashValue 使用 JSON 序列化
 *
 * @author Fu Wei
 */
@EnableCaching
@AutoConfiguration
@ConditionalOnBean(RedisConnectionFactory.class)  // 只有当用户配置了 Redis 连接信息，这个配置类才生效，否则跳过，避免因找不到连接工厂而报错
@ConditionalOnWebApplication(type = SERVLET)  // 只允许在 Servlet 应用中加载该配置，如果是响应式服务（比如网关）则跳过此配置
public class RedisAutoConfiguration {

    /**
     * 创建并配置默认的 RedisTemplate Bean
     *
     * @param connectionFactory Redis 连接工厂
     * @return 配置好的 RedisTemplate 实例
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // 创建 RedisTemplate 实例并设置连接工厂
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        // Key 序列化：使用 String 序列化，便于 redis-cli 直接查看和操作
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setHashKeySerializer(RedisSerializer.string());

        // Value 序列化：使用 JSON 序列化，支持存储复杂对象结构
        redisTemplate.setValueSerializer(RedisSerializer.json());
        redisTemplate.setHashValueSerializer(RedisSerializer.json());

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
