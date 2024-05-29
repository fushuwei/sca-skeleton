package com.itangsoft.skeleton.starter.core.autoconfigure;

import cn.hutool.core.date.DatePattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET;

/**
 * WebMvc自动配置类
 *
 * @author fushuwei
 */
@Configuration
@ConditionalOnWebApplication(type = SERVLET)
public class WebMvcAutoConfiguration implements WebMvcConfigurer {

    /**
     * 增加GET请求参数中时间类型转换 {@link com.itangsoft.skeleton.starter.core.jackson.DefaultSimpleModule}
     *
     * @param registry 格式化程序注册表
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        registrar.setTimeFormatter(DatePattern.NORM_TIME_FORMATTER);
        registrar.setDateFormatter(DatePattern.NORM_DATE_FORMATTER);
        registrar.setDateTimeFormatter(DatePattern.NORM_DATETIME_FORMATTER);
        registrar.registerFormatters(registry);
    }

    /**
     * 系统国际化文件配置
     *
     * @return MessageSource
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        return messageSource;
    }
}
