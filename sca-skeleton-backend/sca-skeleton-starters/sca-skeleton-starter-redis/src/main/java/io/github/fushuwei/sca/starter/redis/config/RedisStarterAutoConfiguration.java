package io.github.fushuwei.sca.starter.redis.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 能力自动配置。
 *
 * @author Fu Wei
 */
@AutoConfiguration
public class RedisStarterAutoConfiguration {

    // 注册统一序列化策略的 RedisTemplate。
    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        // 构造 RedisTemplate 实例并绑定连接工厂。
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // 统一 key 使用字符串序列化，保证可读性。
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        // 统一 value 使用 JSON 序列化，避免 JDK 序列化可读性差。
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);
        // 初始化模板内部配置。
        redisTemplate.afterPropertiesSet();
        // 返回可直接注入使用的模板。
        return redisTemplate;
    }
}
