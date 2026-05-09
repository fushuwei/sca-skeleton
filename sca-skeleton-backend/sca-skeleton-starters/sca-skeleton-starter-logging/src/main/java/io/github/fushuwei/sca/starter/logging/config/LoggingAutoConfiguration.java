package io.github.fushuwei.sca.starter.logging.config;

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

    // Servlet 专用自动配置，避免在纯 WebFlux 场景提前加载 Servlet 类型。
    @AutoConfiguration
    @ConditionalOnClass(name = "jakarta.servlet.http.HttpServletRequest")
    static class ServletLoggingConfiguration {

        // 在 Servlet 环境注册链路追踪过滤器。
        @Bean
        @ConditionalOnMissingBean
        public io.github.fushuwei.sca.starter.logging.web.ServletRequestTraceFilter servletRequestTraceFilter() {
            // 构造 Servlet 侧过滤器实例。
            return new io.github.fushuwei.sca.starter.logging.web.ServletRequestTraceFilter();
        }
    }

    // Gateway/WebFlux 专用自动配置。
    @AutoConfiguration
    @ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GatewayFilterChain")
    static class ReactiveLoggingConfiguration {

        // 仅在 Gateway 环境注册响应式链路追踪过滤器。
        @Bean
        @ConditionalOnMissingBean
        public io.github.fushuwei.sca.starter.logging.web.ReactiveRequestTraceFilter reactiveRequestTraceFilter() {
            // 构造 Gateway 侧全局过滤器实例。
            return new io.github.fushuwei.sca.starter.logging.web.ReactiveRequestTraceFilter();
        }
    }
}
