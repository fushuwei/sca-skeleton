package com.itangsoft.skeleton.starter.core.autoconfigure;

import cn.hutool.core.date.DatePattern;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.itangsoft.skeleton.starter.core.jackson.DefaultSimpleModule;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;
import java.util.TimeZone;

/**
 * Jackson 配置
 *
 * @author fushuwei
 */
@Configuration
@ConditionalOnClass(ObjectMapper.class)
@AutoConfigureBefore(org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class)
public class JacksonAutoConfiguration {

    private static final String ASIA_SHANGHAI = "Asia/Shanghai";

    @Bean
    @ConditionalOnMissingBean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> {
            builder.locale(Locale.CHINA);
            builder.timeZone(TimeZone.getTimeZone(ASIA_SHANGHAI));
            builder.simpleDateFormat(DatePattern.NORM_DATETIME_PATTERN);
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.modules(new DefaultSimpleModule());
        };
    }
}
