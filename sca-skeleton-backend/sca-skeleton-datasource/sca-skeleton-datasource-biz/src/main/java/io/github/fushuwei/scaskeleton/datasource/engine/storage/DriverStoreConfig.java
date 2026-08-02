package io.github.fushuwei.scaskeleton.datasource.engine.storage;

import io.github.fushuwei.scaskeleton.minio.util.MinioUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 驱动存储层配置。
 * <p>
 * 根据 sca.datasource.driver.storage-type 配置选择 LocalDriverStore 或 MinioDriverStore。
 * 开发阶段使用 local（零依赖），生产环境使用 minio（分布式高可用）。
 *
 * @author Fu Wei
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DriverStoreProperties.class)
public class DriverStoreConfig {

    /**
     * 本地驱动存储实现 Bean（开发阶段默认）。
     * <p>
     * 当 storage-type=local 或未配置时激活。
     *
     * @param properties 驱动存储配置属性
     * @return LocalDriverStore 实例
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "sca.datasource.driver", name = "storage-type", havingValue = "local", matchIfMissing = true)
    public DriverStore localDriverStore(DriverStoreProperties properties) {
        return new LocalDriverStore(properties.getBasePath());
    }

    /**
     * MinIO 驱动存储实现 Bean（生产环境）。
     * <p>
     * 当 storage-type=minio 时激活。需要 MinIO 服务可用。
     *
     * @param minioUtils MinIO 工具类（由 sca-skeleton-starter-minio 自动配置）
     * @return MinioDriverStore 实例
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "sca.datasource.driver", name = "storage-type", havingValue = "minio")
    public DriverStore minioDriverStore(MinioUtils minioUtils) {
        return new MinioDriverStore(minioUtils);
    }
}
