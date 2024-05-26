package com.itangsoft.skeleton.starter.core.autoconfigure;

import com.itangsoft.skeleton.starter.core.interceptor.DefaultLocaleChangeInterceptor;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

/**
 * WebMvc自动配置类
 *
 * @author fushuwei
 */
@Configuration
@AutoConfigureBefore({org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration.class})
public class WebMvcAutoConfiguration {

    /**
     * Web属性配置
     *
     * @return WebProperties
     */
    @Bean
    public WebProperties webProperties() {
        WebProperties webProperties = new WebProperties();
        webProperties.setLocale(Locale.SIMPLIFIED_CHINESE);
        return webProperties;
    }

    /**
     * 国际化区域设置解析器
     *
     * @return LocaleResolver
     */
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver localeResolver = new SessionLocaleResolver();
        localeResolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        return localeResolver;
    }

    /**
     * WebMvc配置
     *
     * @return WebMvcConfigurer
     */
    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            /**
             * 配置拦截器
             *
             * @param registry 拦截器注册表
             */
            @Override
            public void addInterceptors(@NonNull InterceptorRegistry registry) {
                registry.addInterceptor(new DefaultLocaleChangeInterceptor());
            }
        };
    }
}
