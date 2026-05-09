package io.github.fushuwei.sca.starter.web.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Web MVC 通用配置。
 * <p>
 * 负责 Jackson 全局日期格式统一（yyyy-MM-dd HH:mm:ss）、宽松反序列化，
 * 以及默认 CORS 策略（生产环境由 API 网关统一收口，此处仅兜底）。
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
     * 全局 Jackson ObjectMapper 配置。使用 ConditionalOnMissingBean 确保业务模块可覆盖。
     */
    @Bean
    @ConditionalOnMissingBean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        // 注册 Java 8 时间模块，统一 LocalDate / LocalDateTime 序列化格式
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        // LocalDateTime 序列化与反序列化
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));
        // LocalDate 序列化与反序列化
        javaTimeModule.addSerializer(LocalDate.class,
                new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));
        javaTimeModule.addDeserializer(LocalDate.class,
                new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));

        return new Jackson2ObjectMapperBuilder()
                .modules(javaTimeModule)
                // 禁止日期序列化为时间戳数字
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // 忽略 JSON 中存在但 Java 对象没有的未知字段，提升接口版本兼容性
                .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // 避免空对象抛出 SerializationException
                .featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
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
