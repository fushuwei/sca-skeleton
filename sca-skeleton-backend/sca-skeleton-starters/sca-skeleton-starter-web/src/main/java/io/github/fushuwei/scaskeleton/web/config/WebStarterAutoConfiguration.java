package io.github.fushuwei.scaskeleton.web.config;

import io.github.fushuwei.scaskeleton.web.exception.GlobalExceptionHandler;
import io.github.fushuwei.scaskeleton.web.filter.TraceIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * Web Starter 自动配置入口。
 * <p>
 * 统一注册：全局异常处理器、TraceId 注入过滤器；Jackson 全局约定由 starter-core 提供。
 *
 * @author Fu Wei
 */
@AutoConfiguration
@Import(GlobalExceptionHandler.class)
public class WebStarterAutoConfiguration {

    /**
     * 注册 TraceId 过滤器，设置最高优先级确保所有请求在进入业务逻辑前已完成链路 ID 注入。
     *
     * @return FilterRegistrationBean 过滤器注册包装器
     */
    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>();
        // 包装 TraceId 过滤器实例
        registration.setFilter(new TraceIdFilter());
        // 拦截所有请求路径
        registration.addUrlPatterns("/*");
        // 设置最高优先级，确保链路 ID 在最早阶段注入
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("traceIdFilter");
        return registration;
    }
}
