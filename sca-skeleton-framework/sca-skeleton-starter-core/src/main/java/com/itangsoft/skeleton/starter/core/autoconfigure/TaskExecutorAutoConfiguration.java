package com.itangsoft.skeleton.starter.core.autoconfigure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * TaskExecutor 自动配置
 *
 * @author fushuwei
 */
@Configuration
public class TaskExecutorAutoConfiguration implements AsyncConfigurer {

    /**
     * 获取当前机器的核数, 不一定准确, 根据实际场景判断: CPU密集型 | IO密集型
     */
    public static final int cpuNum = Runtime.getRuntime().availableProcessors();

    @Value("${thread.pool.corePoolSize:}")
    private Optional<Integer> corePoolSize;

    @Value("${thread.pool.maxPoolSize:}")
    private Optional<Integer> maxPoolSize;

    @Value("${thread.pool.queueCapacity:}")
    private Optional<Integer> queueCapacity;

    @Value("${thread.pool.awaitTerminationSeconds:}")
    private Optional<Integer> awaitTerminationSeconds;

    @Override
    @Bean
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        // 核心线程大小 默认区 CPU 数量
        taskExecutor.setCorePoolSize(corePoolSize.orElse(cpuNum));
        // 最大线程大小 默认区 CPU * 2 数量
        taskExecutor.setMaxPoolSize(maxPoolSize.orElse(cpuNum * 2));
        // 队列最大容量
        taskExecutor.setQueueCapacity(queueCapacity.orElse(500));
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setAwaitTerminationSeconds(awaitTerminationSeconds.orElse(60));
        taskExecutor.setThreadNamePrefix("PIG-Thread-");
        taskExecutor.initialize();
        return taskExecutor;
    }
}
