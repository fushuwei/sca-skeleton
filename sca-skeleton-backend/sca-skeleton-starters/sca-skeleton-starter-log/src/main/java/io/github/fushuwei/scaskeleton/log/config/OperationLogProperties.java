package io.github.fushuwei.scaskeleton.log.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 操作日志配置属性类
 * <p>
 * 通过 {@code application.yml} 调整异步线程池参数：
 * <pre>
 * sca:
 *   operation-log:
 *     executor:
 *       core-pool-size: 2
 *       max-pool-size: 4
 *       queue-capacity: 5000
 *       await-termination-seconds: 30
 * </pre>
 *
 * @author Fu Wei
 */
@Getter
@ConfigurationProperties(prefix = "sca.operation-log")
public class OperationLogProperties {

    /**
     * 线程池配置
     */
    private final Executor executor = new Executor();

    @Setter
    @Getter
    public static class Executor {

        /**
         * 核心线程数
         */
        private int corePoolSize = 2;

        /**
         * 最大线程数
         */
        private int maxPoolSize = 4;

        /**
         * 队列容量
         */
        private int queueCapacity = 5000;

        /**
         * 优雅关闭等待时间（秒），确保队列中剩余日志处理完毕
         */
        private int awaitTerminationSeconds = 30;

    }
}
