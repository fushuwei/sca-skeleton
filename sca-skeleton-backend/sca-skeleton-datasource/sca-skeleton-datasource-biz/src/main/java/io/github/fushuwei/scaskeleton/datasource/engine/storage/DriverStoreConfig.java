package io.github.fushuwei.scaskeleton.datasource.engine.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 驱动存储层配置。
 * <p>
 * 根据 sca.datasource.driver.storage-type 配置选择 LocalDriverStore 或 MinioDriverStore。
 * 首期（M0/M1）仅实现 LocalDriverStore，MinIO 在 M3/M4 引入。
 *
 * @author Fu Wei
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DriverStoreProperties.class)
public class DriverStoreConfig {

    /**
     * 本地驱动存储实现 Bean（默认）。
     * <p>
     * 当 storage-type=local 时激活。
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
}
