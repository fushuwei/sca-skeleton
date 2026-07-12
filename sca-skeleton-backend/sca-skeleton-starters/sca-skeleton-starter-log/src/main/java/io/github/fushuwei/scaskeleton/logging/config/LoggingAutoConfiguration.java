package io.github.fushuwei.scaskeleton.logging.config;

import tools.jackson.databind.json.JsonMapper;
import io.github.fushuwei.scaskeleton.core.user.CurrentUserProvider;
import io.github.fushuwei.scaskeleton.logging.aspect.OperationLogAspect;
import io.github.fushuwei.scaskeleton.logging.event.OperationLogEventListener;
import io.github.fushuwei.scaskeleton.logging.handler.DbOperationLogHandler;
import io.github.fushuwei.scaskeleton.logging.handler.OperationLogHandler;
import io.github.fushuwei.scaskeleton.logging.mapper.SysOperationLogMapper;
import lombok.extern.slf4j.Slf4j;
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

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 操作日志自动配置。
 * <p>
 * 装配切面、异步线程池、事件监听器、DB 持久化 Handler。引入本 starter 后，
 * 标注 {@code @OperationLog} 即可自动异步入库，无需额外配置。
 * <p>
 * 将来升级 MQ：替换本模块的 Listener 为 MQ Producer，切面代码零改动。
 *
 * @author Fu Wei
 */
@Slf4j
@AutoConfiguration
@EnableAsync
@EnableConfigurationProperties(OperationLogProperties.class)
@MapperScan(basePackageClasses = SysOperationLogMapper.class)
public class LoggingAutoConfiguration {

    // ── 线程池 ──

    /** 操作日志专用线程池，独立于 Spring 默认 taskExecutor */
    @Bean("operationLogExecutor")
    @ConditionalOnMissingBean(name = "operationLogExecutor")
    public Executor operationLogExecutor(OperationLogProperties properties) {
        OperationLogProperties.Executor props = properties.getExecutor();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getCorePoolSize());
        executor.setMaxPoolSize(props.getMaxPoolSize());
        executor.setQueueCapacity(props.getQueueCapacity());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("op-log-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(props.getAwaitTerminationSeconds());
        return executor;
    }

    // ── 切面 ──

    @Bean
    @ConditionalOnMissingBean
    public OperationLogAspect operationLogAspect(
            JsonMapper jsonMapper,
            ApplicationEventPublisher eventPublisher,
            @Autowired(required = false) @Nullable CurrentUserProvider currentUserProvider) {
        return new OperationLogAspect(jsonMapper, eventPublisher, currentUserProvider);
    }

    // ── Handler ──

    @Bean
    @ConditionalOnMissingBean
    public OperationLogHandler operationLogHandler(SysOperationLogMapper mapper) {
        return new DbOperationLogHandler(mapper);
    }

    // ── 异步监听器 ──

    @Bean
    @ConditionalOnMissingBean
    public OperationLogEventListener operationLogEventListener(
            @Autowired(required = false) @Nullable OperationLogHandler operationLogHandler) {
        return new OperationLogEventListener(operationLogHandler);
    }

    // ── MDC 透传 ──

    /**
     * 将主线程的 MDC（含 traceId）拷贝到异步线程，确保异步线程中的日志也能关联链路追踪 ID。
     */
    static class MdcTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
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
