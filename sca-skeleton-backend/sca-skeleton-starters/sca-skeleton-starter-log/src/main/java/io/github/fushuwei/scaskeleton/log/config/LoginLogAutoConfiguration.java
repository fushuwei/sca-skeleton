package io.github.fushuwei.scaskeleton.log.config;

import io.github.fushuwei.scaskeleton.log.event.LoginLogEventListener;
import io.github.fushuwei.scaskeleton.log.handler.DefaultLoginLogHandler;
import io.github.fushuwei.scaskeleton.log.handler.LoginLogHandler;
import io.github.fushuwei.scaskeleton.log.mapper.SysLoginLogMapper;
import io.github.fushuwei.scaskeleton.log.support.Ip2RegionResolver;
import io.github.fushuwei.scaskeleton.log.support.IpRegionResolver;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 登录日志自动配置类
 *
 * @author Fu Wei
 */
@Slf4j
@AutoConfiguration
@EnableAsync
@EnableConfigurationProperties(LoginLogProperties.class)
public class LoginLogAutoConfiguration {

    /**
     * 登录日志专用线程池，独立于 Spring 默认 taskExecutor
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
     * 登录日志处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public LoginLogHandler loginLogHandler(SysLoginLogMapper loginLogMapper) {
        return new DefaultLoginLogHandler(loginLogMapper);
    }

    /**
     * IP 地理位置解析器
     */
    @Bean
    @ConditionalOnMissingBean(IpRegionResolver.class)
    public IpRegionResolver ipRegionResolver(LoginLogProperties properties) {
        String dbPath = properties.getIpRegion().getDbPath();
        if (!StringUtils.hasText(dbPath)) {
            log.warn("[登录日志] 未配置 ip2region 数据库路径，location 字段不会被解析");
            return ip -> null;
        }
        Path path = Path.of(dbPath);
        if (!Files.exists(path)) {
            log.warn("[登录日志] ip2region 数据库文件不存在: {}，location 字段不会被解析", dbPath);
            return ip -> null;
        }
        try {
            log.info("[登录日志] 加载 ip2region 数据库: {}", dbPath);
            return Ip2RegionResolver.create(dbPath);
        } catch (Exception e) {
            log.warn("[登录日志] ip2region 数据库加载失败，location 字段不会被解析", e);
            return ip -> null;
        }
    }

    /**
     * 登录日志异步持久化监听器
     */
    @Bean
    @ConditionalOnMissingBean
    public LoginLogEventListener loginLogEventListener(
        @Autowired(required = false) @Nullable LoginLogHandler loginLogHandler, IpRegionResolver ipRegionResolver) {
        return new LoginLogEventListener(loginLogHandler, ipRegionResolver);
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
