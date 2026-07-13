package io.github.fushuwei.scaskeleton.log.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 登录日志配置属性类
 * <p>
 * 通过 {@code application.yml} 调整异步线程池参数与 IP 地理位置解析：
 * <pre>
 * sca:
 *   login-log:
 *     executor:
 *       core-pool-size: 2
 *       max-pool-size: 4
 *       queue-capacity: 5000
 *       await-termination-seconds: 30
 *     ip-region:
 *       db-path: ./ip2region/ip2region_v4.xdb
 * </pre>
 *
 * @author Fu Wei
 */
@Getter
@ConfigurationProperties(prefix = "sca.login-log")
public class LoginLogProperties {

    /**
     * 线程池配置
     */
    private final Executor executor = new Executor();

    /**
     * IP 地理位置解析配置
     */
    private final IpRegion ipRegion = new IpRegion();

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

    @Setter
    @Getter
    public static class IpRegion {

        /**
         * ip2region xdb 数据库文件路径
         * <p>
         * xdb 文件下载地址: <a href="https://github.com/lionsoul2014/ip2region/blob/master/data/ip2region_v4.xdb">ip2region_v4.xdb</a>
         */
        private String dbPath = "./ip2region/ip2region_v4.xdb";

    }
}
