package io.github.fushuwei.scaskeleton.log.config;

import io.github.fushuwei.scaskeleton.core.ip.IpRegionResolver;
import io.github.fushuwei.scaskeleton.core.user.CurrentUserProvider;
import io.github.fushuwei.scaskeleton.log.aspect.OperationLogAspect;
import io.github.fushuwei.scaskeleton.log.event.OperationLogEventListener;
import io.github.fushuwei.scaskeleton.log.handler.DefaultOperationLogHandler;
import io.github.fushuwei.scaskeleton.log.handler.OperationLogHandler;
import io.github.fushuwei.scaskeleton.log.mapper.SysOperationLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 操作日志自动配置类
 *
 * @author Fu Wei
 */
@Slf4j
@AutoConfiguration
@EnableAsync
@EnableConfigurationProperties(OperationLogProperties.class)
@MapperScan(basePackageClasses = SysOperationLogMapper.class)
public class OperationLogAutoConfiguration {

    /**
     * 操作日志专用线程池，独立于 Spring 默认 taskExecutor
     */
    @Bean("operationLogExecutor")
    @ConditionalOnMissingBean(name = "operationLogExecutor")
    public Executor operationLogExecutor(OperationLogProperties properties) {
        OperationLogProperties.Executor props = properties.getExecutor();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getCorePoolSize());
        executor.setMaxPoolSize(props.getMaxPoolSize());
        executor.setQueueCapacity(props.getQueueCapacity());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("operation-log-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(props.getAwaitTerminationSeconds());
        return executor;
    }

    /**
     * 操作日志切面
     */
    @Bean
    @ConditionalOnMissingBean
    public OperationLogAspect operationLogAspect(JsonMapper jsonMapper, ApplicationEventPublisher eventPublisher,
                                                 @Autowired(required = false) @Nullable CurrentUserProvider currentUserProvider) {
        return new OperationLogAspect(jsonMapper, eventPublisher, currentUserProvider);
    }

    /**
     * 操作日志处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public OperationLogHandler operationLogHandler(SysOperationLogMapper operationLogMapper) {
        return new DefaultOperationLogHandler(operationLogMapper);
    }

    /**
     * 操作日志异步持久化监听器
     */
    @Bean
    @ConditionalOnMissingBean
    public OperationLogEventListener operationLogEventListener(
        @Autowired(required = false) @Nullable OperationLogHandler operationLogHandler, IpRegionResolver ipRegionResolver) {
        return new OperationLogEventListener(operationLogHandler, ipRegionResolver);
    }

    /**
     * 将主线程的 MDC（含 traceId）拷贝到异步线程，确保异步线程中的日志也能关联链路追踪 ID
     */
    static class MdcTaskDecorator implements TaskDecorator {
        @Override
        public @NullMarked Runnable decorate(Runnable runnable) {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                try {
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        }
    }
}
