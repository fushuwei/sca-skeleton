package io.github.fushuwei.sca.starter.logging.config;

import io.github.fushuwei.sca.starter.logging.web.ReactiveRequestTraceFilter;
import io.github.fushuwei.sca.starter.logging.web.ServletRequestTraceFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 日志追踪自动配置。
 *
 * @author Fu Wei
 */
@AutoConfiguration
public class LoggingAutoConfiguration {

    // 在 Servlet 环境下注册请求追踪过滤器。
    @Bean
    @ConditionalOnClass(ServletRequestTraceFilter.class)
    @ConditionalOnMissingBean
    public ServletRequestTraceFilter servletRequestTraceFilter() {
        // 创建 Servlet 请求追踪过滤器实例。
        return new ServletRequestTraceFilter();
    }

    // 在 Reactive 环境下注册请求追踪过滤器。
    @Bean
    @ConditionalOnClass(ReactiveRequestTraceFilter.class)
    @ConditionalOnMissingBean
    public ReactiveRequestTraceFilter reactiveRequestTraceFilter() {
        // 创建 Reactive 请求追踪过滤器实例。
        return new ReactiveRequestTraceFilter();
    }
}
