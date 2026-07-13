package io.github.fushuwei.scaskeleton.log.config;

import io.github.fushuwei.scaskeleton.log.event.LoginLogEventListener;
import io.github.fushuwei.scaskeleton.log.mapper.SysLoginLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 登录日志自动配置：线程池 + 异步监听器。
 * <p>
 * 通过 {@code @EnableAsync} 启用异步支持，注册专用线程池 {@code loginLogExecutor}
 * 与 {@link LoginLogEventListener} Bean。业务层只需发布 {@link io.github.fushuwei.scaskeleton.log.event.LoginLogEvent}
 * 即可触发异步落库，无需感知底层实现。
 *
 * @author Fu Wei
 */
@Slf4j
@AutoConfiguration
@EnableAsync
@EnableConfigurationProperties(LoginLogProperties.class)
public class LoginLogAutoConfiguration {

    /**
     * 登录日志专用线程池，独立于操作日志线程池与 Spring 默认 taskExecutor
     */
    @Bean("loginLogExecutor")
    @ConditionalOnMissingBean(name = "loginLogExecutor")
    public Executor loginLogExecutor(LoginLogProperties properties) {
        LoginLogProperties.Executor props = properties.getExecutor();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getCorePoolSize());
        executor.setMaxPoolSize(props.getMaxPoolSize());
        executor.setQueueCapacity(props.getQueueCapacity());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("login-log-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(props.getAwaitTerminationSeconds());
        return executor;
    }

    /**
     * 登录日志异步持久化监听器
     */
    @Bean
    @ConditionalOnMissingBean
    public LoginLogEventListener loginLogEventListener(SysLoginLogMapper loginLogMapper) {
        return new LoginLogEventListener(loginLogMapper);
    }

    /**
     * 将主线程的 MDC（含 traceId）拷贝到异步线程，确保异步线程中的日志也能关联链路追踪 ID
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
