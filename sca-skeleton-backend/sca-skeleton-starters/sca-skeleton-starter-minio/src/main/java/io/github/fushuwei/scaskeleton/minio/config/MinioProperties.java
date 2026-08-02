package io.github.fushuwei.scaskeleton.minio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO 配置属性
 *
 * @author Fu Wei
 */
@Data
@ConfigurationProperties(prefix = "sca.minio")
public class MinioProperties {

    /** MinIO 服务端点（如 http://127.0.0.1:9000） */
    private String endpoint;

    /** 访问密钥 */
    private String accessKey;

    /** 秘密密钥 */
    private String secretKey;

    /** 默认 Bucket 名称（如 sca-skeleton） */
    private String bucket;

    /** 是否启用 MinIO（默认 true，配置缺失时不创建 MinioClient Bean） */
    private boolean enabled = true;
}
