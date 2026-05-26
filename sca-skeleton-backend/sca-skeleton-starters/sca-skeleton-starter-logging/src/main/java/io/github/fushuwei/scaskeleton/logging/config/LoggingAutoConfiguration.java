package io.github.fushuwei.scaskeleton.logging.config;

import tools.jackson.databind.ObjectMapper;
import io.github.fushuwei.scaskeleton.core.user.CurrentUserProvider;
import io.github.fushuwei.scaskeleton.logging.aspect.OperationLogAspect;
import io.github.fushuwei.scaskeleton.logging.handler.OperationLogHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.Nullable;

/**
 * Logging Starter 自动配置入口。
 * <p>
 * 注册 {@link OperationLogAspect} 切面 Bean，
 * 依赖 ObjectMapper（由 Web Starter 或 Spring Boot 自动装配）、
 * 可选的 {@link CurrentUserProvider} 与 {@link OperationLogHandler}。
 *
 * @author Fu Wei
 */
@AutoConfiguration
public class LoggingAutoConfiguration {

    /**
     * 注册操作日志切面，注入可选依赖（CurrentUserProvider、OperationLogHandler）。
     * 使用 ConditionalOnMissingBean 允许业务模块自定义切面实现。
     *
     * @param objectMapper         Jackson 序列化器，用于序列化请求参数与响应结果
     * @param currentUserProvider  当前用户信息提供者（可选，由 Security Starter 提供）
     * @param operationLogHandler  操作日志处理器（可选，由业务模块提供）
     * @return 操作日志切面 Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public OperationLogAspect operationLogAspect(
            ObjectMapper objectMapper,
            @Autowired(required = false) @Nullable CurrentUserProvider currentUserProvider,
            @Autowired(required = false) @Nullable OperationLogHandler operationLogHandler) {
        // 组装操作日志切面：序列化参数/响应，可选注入当前用户与自定义日志处理器
        return new OperationLogAspect(objectMapper, currentUserProvider, operationLogHandler);
    }
}
