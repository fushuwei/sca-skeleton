package io.github.fushuwei.scaskeleton.datasource.engine.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 驱动存储配置属性。
 *
 * @author Fu Wei
 */
@Data
@ConfigurationProperties(prefix = "sca.datasource.driver")
public class DriverStoreProperties {

    /** 存储类型（local/minio），默认 local */
    private String storageType = "local";

    /** 本地存储根路径，默认 ~/.sca-skeleton/drivers */
    private String basePath = System.getProperty("user.home") + "/.sca-skeleton/drivers";
}
