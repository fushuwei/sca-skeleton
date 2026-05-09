package io.github.fushuwei.sca.starter.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisTemplate 序列化配置。
 * <p>
 * Key 使用 {@link StringRedisSerializer}，Value 使用 Jackson JSON 序列化并激活类型信息写入，
 * 确保反序列化时能正确还原复杂 Java 类型。使用 ConditionalOnMissingBean 允许业务模块覆盖。
 *
 * @author Fu Wei
 */
@Configuration
public class RedisTemplateConfig {

    /**
     * 配置 RedisTemplate：Key 字符串序列化，Value Jackson JSON 序列化（含 @class 类型信息）。
     *
     * @param connectionFactory Redis 连接工厂，由 Spring Boot 自动配置提供
     * @return 配置好的 RedisTemplate
     */
    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // 构造带类型信息的 ObjectMapper，使 JSON 中含 @class 字段支持多态反序列化
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // 使用上述 ObjectMapper 构造 Jackson 序列化器
        Jackson2JsonRedisSerializer<Object> jsonSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        // Key / HashKey 使用字符串序列化，方便在 Redis CLI 直接查看
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        // Value / HashValue 使用 JSON 序列化，支持复杂对象存取
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
