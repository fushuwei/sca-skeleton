package io.github.fushuwei.sca.starter.feign.config;

import feign.codec.ErrorDecoder;
import io.github.fushuwei.sca.starter.feign.decoder.FeignErrorDecoder;
import io.github.fushuwei.sca.starter.feign.interceptor.FeignHeaderInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Feign Starter 自动配置入口。
 * <p>
 * 统一注册：请求头传递拦截器（TraceId/Authorization）、统一错误解码器。
 * 使用 ConditionalOnClass 确保只在 OpenFeign 存在时才激活配置。
 *
 * @author Fu Wei
 */
@AutoConfiguration
@ConditionalOnClass(name = "feign.RequestInterceptor")
public class FeignAutoConfiguration {

    /**
     * 注册请求头传递拦截器，所有 Feign 客户端均自动生效（全局拦截器）。
     *
     * @return FeignHeaderInterceptor 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public FeignHeaderInterceptor feignHeaderInterceptor() {
        return new FeignHeaderInterceptor();
    }

    /**
     * 注册统一 Feign 错误解码器，将下游非 2xx 响应映射为 BusinessException。
     *
     * @return FeignErrorDecoder 实例
     */
    @Bean
    @ConditionalOnMissingBean(ErrorDecoder.class)
    public FeignErrorDecoder feignErrorDecoder() {
        return new FeignErrorDecoder();
    }
}
