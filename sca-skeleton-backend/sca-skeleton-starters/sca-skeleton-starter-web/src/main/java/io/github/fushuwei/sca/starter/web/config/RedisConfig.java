package io.github.fushuwei.sca.starter.web.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class RedisConfig {

    /**
     * 自定义 RedisTemplate
     * <p>
     * Key / HashKey 使用 String 序列化
     * Value 使用 JSON 序列化（后续存对象方便）
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());

        template.setValueSerializer(RedisSerializer.json());
        template.setHashValueSerializer(RedisSerializer.json());

        template.afterPropertiesSet();
        return template;
    }


    /** 标准日期时间格式 */
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /** 标准日期格式 */
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    /**
     * 全局 Jackson JsonMapper 定制器。
     * Spring Boot 4 会将所有 {@link JsonMapperBuilderCustomizer} 累加应用到自动配置的 JsonMapper 上，
     * 业务模块若需进一步定制，再额外注册 Customizer Bean 即可（按 @Order 排序）。
     */
    @Bean
    public JsonMapperBuilderCustomizer scaJsonMapperBuilderCustomizer() {
        return builder -> {
            // 自定义 Java 8 时间序列化格式（Jackson 3 已内置 JSR310 支持，仅覆盖格式）
            SimpleModule javaTimeModule = new SimpleModule("sca-java-time");
            javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));
            javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));
            javaTimeModule.addSerializer(LocalDate.class,
                new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));
            javaTimeModule.addDeserializer(LocalDate.class,
                new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));

            // Jackson 3 已默认不再将日期序列化为时间戳（WRITE_DATES_AS_TIMESTAMPS 枚举已移除），无需显式 disable
            builder.addModule(javaTimeModule)
                // 忽略 JSON 中存在但 Java 对象没有的未知字段，提升接口版本兼容性
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // 避免空对象抛出 SerializationException
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        };
    }


}
