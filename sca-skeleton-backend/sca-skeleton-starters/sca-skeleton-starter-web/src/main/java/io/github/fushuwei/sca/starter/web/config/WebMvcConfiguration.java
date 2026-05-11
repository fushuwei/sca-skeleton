package io.github.fushuwei.sca.starter.web.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
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
 * Web MVC 通用配置。
 * <p>
 * 负责 Jackson 全局日期格式统一（yyyy-MM-dd HH:mm:ss）、宽松反序列化，
 * 以及默认 CORS 策略（生产环境由 API 网关统一收口，此处仅兜底）。
 * <p>
 * Jackson 3（Spring Boot 4）通过 {@link JsonMapperBuilderCustomizer} 叠加方式定制
 * 自动配置的 {@code JsonMapper}，业务模块可再注册 Customizer 进一步覆盖。
 *
 * @author Fu Wei
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

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

    /**
     * 默认 CORS 策略，服务本地开发与集成测试场景。
     * 生产环境应由 API 网关统一管控 CORS，可在业务模块声明 WebMvcConfigurer Bean 覆盖此配置。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 允许所有来源，生产时应限定具体域名
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                // 允许携带认证信息（Cookie、Authorization 等）
                .allowCredentials(true)
                // 预检请求缓存 1 小时，减少 OPTIONS 请求频次
                .maxAge(3600);
    }
}
