package io.github.fushuwei.scaskeleton.core.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis 自动配置类
 * <p>
 * 启用 Spring 缓存抽象，并配置默认的 RedisTemplate Bean，
 * Key 与 HashKey 使用 String 序列化，Value 与 HashValue 使用 JSON 序列化
 *
 * @author Fu Wei
 */
@EnableCaching
@AutoConfiguration
public class RedisConfiguration {

    /**
     * 创建并配置默认的 RedisTemplate Bean
     *
     * @param factory Redis 连接工厂
     * @return 配置好的 RedisTemplate 实例
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        // 创建 RedisTemplate 实例并设置连接工厂
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);

        // Key 序列化：使用 String 序列化，便于 Redis 命令行直接阅读和操作
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setHashKeySerializer(RedisSerializer.string());

        // Value 序列化：使用 JSON 序列化，支持存储复杂对象结构
        redisTemplate.setValueSerializer(RedisSerializer.json());
        redisTemplate.setHashValueSerializer(RedisSerializer.json());

        return redisTemplate;
    }
}
