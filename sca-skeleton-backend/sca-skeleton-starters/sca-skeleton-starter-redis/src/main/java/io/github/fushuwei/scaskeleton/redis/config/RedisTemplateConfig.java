package io.github.fushuwei.scaskeleton.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * RedisTemplate 序列化配置。
 * <p>
 * Key 使用 {@link StringRedisSerializer}，Value 使用 Jackson 3 JSON 序列化并激活类型信息写入，
 * 确保反序列化时能正确还原复杂 Java 类型。使用 ConditionalOnMissingBean 允许业务模块覆盖。
 * <p>
 * Jackson 3 强制使用 {@link JsonMapper.Builder} 配置 ObjectMapper：
 * {@code setVisibility}、{@code activateDefaultTyping} 等方法已从 ObjectMapper 移除，仅在 Builder 上提供。
 *
 * @author Fu Wei
 */
@Configuration(proxyBeanMethods = false)
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
        // Jackson 3 不再公开 LaissezFaireSubTypeValidator，使用 BasicPolymorphicTypeValidator 放行 Object 全部子类，
        // 等价于 Jackson 2 中 LaissezFaireSubTypeValidator 的“无校验”语义（仍受 Default Typing 范围限制）。
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class)
                .build();

        // 通过 JsonMapper.Builder 构造带多态类型信息的 ObjectMapper（Jackson 3 仅 Builder 暴露这些 API）
        ObjectMapper objectMapper = JsonMapper.builder()
                // 所有字段（含 private）参与序列化，匹配 Jackson 2 时代的 PropertyAccessor.ALL + Visibility.ANY 行为
                .changeDefaultVisibility(checker -> checker
                        .withVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY))
                // 激活 NON_FINAL 默认 Typing，使 JSON 中带 @class 字段以支持多态反序列化
                .activateDefaultTyping(typeValidator, DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
                .build();

        // Spring Data Redis 4.0 的 Jackson 3 序列化器（替代 Jackson2JsonRedisSerializer）
        JacksonJsonRedisSerializer<Object> jsonSerializer =
                new JacksonJsonRedisSerializer<>(objectMapper, Object.class);

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
