package io.github.fushuwei.scaskeleton.core.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
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

/**
 * Jackson 3（Spring Boot 4）全局 {@link JsonMapperBuilderCustomizer}。
 * <p>
 * 通过 Boot 自动配置将定制逻辑叠加到应用主 {@code JsonMapper} 上，供 Web、Redis JSON、
 * 安全响应体等共用；业务模块可再注册其他 Customizer（按 {@code @Order} 排序）进一步覆盖。
 * <p>
 * 依赖由 {@code spring-boot-starter-jackson} 提供，本类在 {@link JacksonAutoConfiguration} 之后执行，
 * 以便在默认模块之上追加项目级约定。
 *
 * @author Fu Wei
 */
@AutoConfiguration
@AutoConfigureAfter(JacksonAutoConfiguration.class)
public class Jackson3AutoConfiguration {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 统一 Java 8 日期时间格式、宽松反序列化及空 Bean 序列化策略。
     *
     * @return 应用于全局 JsonMapper 的定制器
     */
    @Bean
    public JsonMapperBuilderCustomizer scaCoreJacksonJsonMapperBuilderCustomizer() {
        return builder -> {
            SimpleModule javaTimeModule = new SimpleModule("sca-java-time");
            javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
            javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
            javaTimeModule.addSerializer(LocalDate.class,
                new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));
            javaTimeModule.addDeserializer(LocalDate.class,
                new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));

            builder.addModule(javaTimeModule)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        };
    }
}
