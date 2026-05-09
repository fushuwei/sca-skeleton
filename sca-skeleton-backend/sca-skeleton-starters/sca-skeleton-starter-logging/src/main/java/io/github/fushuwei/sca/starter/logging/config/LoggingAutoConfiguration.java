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

    // 在 Servlet 环境注册链路追踪过滤器，避免在纯 WebFlux 无 Servlet 时误注册。
    @Bean
    @ConditionalOnClass(name = "jakarta.servlet.http.HttpServletRequest")
    @ConditionalOnMissingBean
    public ServletRequestTraceFilter servletRequestTraceFilter() {
        // 构造 Servlet 侧过滤器实例。
        return new ServletRequestTraceFilter();
    }

    // 仅在 Spring Cloud Gateway 存在时注册响应式链路追踪过滤器，避免普通 Servlet 应用强依赖 Gateway。
    @Bean
    @ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GatewayFilterChain")
    @ConditionalOnMissingBean
    public ReactiveRequestTraceFilter reactiveRequestTraceFilter() {
        // 构造 Gateway 侧全局过滤器实例。
        return new ReactiveRequestTraceFilter();
    }
}
