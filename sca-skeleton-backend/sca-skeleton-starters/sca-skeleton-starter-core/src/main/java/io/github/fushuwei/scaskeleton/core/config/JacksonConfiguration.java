package io.github.fushuwei.scaskeleton.core.config;

import io.github.fushuwei.scaskeleton.core.jackson.JavaLangModule;
import io.github.fushuwei.scaskeleton.core.jackson.JavaTimeModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Jackson 3 全局配置
 * <p>
 * 本类在 {@link JacksonAutoConfiguration} 之后执行，以便在默认模块之上追加项目级约定
 *
 * @author Fu Wei
 */
@AutoConfiguration
@AutoConfigureAfter(JacksonAutoConfiguration.class)
public class JacksonConfiguration {

    /**
     * 统一序列化/反序列化策略
     *
     * @return 应用于全局 JsonMapper 的定制器
     */
    @Bean
    @ConditionalOnMissingBean
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
