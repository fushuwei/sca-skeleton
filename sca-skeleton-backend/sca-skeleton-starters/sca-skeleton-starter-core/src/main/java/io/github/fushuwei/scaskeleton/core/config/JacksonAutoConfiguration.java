package io.github.fushuwei.scaskeleton.core.config;

import io.github.fushuwei.scaskeleton.core.jackson.JavaLangModule;
import io.github.fushuwei.scaskeleton.core.jackson.JavaTimeModule;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Jackson3 自动配置类
 *
 * @author Fu Wei
 */
@AutoConfiguration
public class JacksonAutoConfiguration {

    /**
     * 全局通用 JsonMapper
     * <p>
     * 收集容器中所有 {@link JsonMapperBuilderCustomizer}，根据 @Order 执行顺序排序后逐个应用到 {@link JsonMapper.Builder}
     *
     * @param customizers 所有 Jackson 策略定制器的 ObjectProvider
     * @return 全局通用 JsonMapper
     */
    @Bean
    @Primary
    public JsonMapper jsonMapper(ObjectProvider<JsonMapperBuilderCustomizer> customizers) {
        JsonMapper.Builder builder = JsonMapper.builder();
        customizers.orderedStream().forEach(c -> c.customize(builder));
        return builder.build();
    }

    /**
     * Jackson 统一序列化/反序列化策略定制器
     * <p>
     * 排在所有内置 Customizer 之后执行，确保自定义模块与 Feature 配置不会被覆盖
     *
     * @return 应用于 JsonMapper 的定制器
     */
    @Bean
    @Order
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        return builder -> {
            // 时区
            builder.defaultTimeZone(TimeZone.getTimeZone(ZoneId.systemDefault()));

            // java.time 类型序列化/反序列化
            builder.addModule(new JavaTimeModule());

            // java.lang 类型序列化/反序列化
            builder.addModule(new JavaLangModule());

            // 反序列化：JSON 多出的字段在 Java 类型上无对应属性时不报错
            builder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            // 序列化：空对象不报错，仍输出 {}
            builder.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        };
    }
}
