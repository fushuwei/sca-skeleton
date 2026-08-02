package io.github.fushuwei.scaskeleton.minio.config;

import io.github.fushuwei.scaskeleton.minio.util.MinioUtils;
import io.minio.MinioClient;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * MinIO 自动配置。
 * <p>
 * 仅当显式配置 {@code sca.minio.enabled=true} 且 endpoint 非空时才创建 MinioClient。
 * 未配置或 enabled=false 时跳过，不报错、不阻断启动——相关功能在使用时提示「MinIO 未配置」。
 *
 * @author Fu Wei
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MinioClient.class)
@ConditionalOnProperty(prefix = "sca.minio", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(MinioProperties.class)
public class MinioAutoConfiguration {

    /**
     * 创建 MinioClient Bean。
     * <p>
     * 仅当 endpoint 非空时才创建，否则跳过。
     *
     * @param properties MinIO 配置属性
     * @return MinioClient 实例
     */
    @Bean
    @ConditionalOnProperty(prefix = "sca.minio", name = "endpoint")
    public MinioClient minioClient(MinioProperties properties) {
        MinioClient client = MinioClient.builder()
            .endpoint(properties.getEndpoint())
            .credentials(properties.getAccessKey(), properties.getSecretKey())
            .build();

        // 确保 Bucket 存在
        try {
            boolean exists = client.bucketExists(
                BucketExistsArgs.builder().bucket(properties.getBucket()).build());
            if (!exists) {
                client.makeBucket(
                    MakeBucketArgs.builder().bucket(properties.getBucket()).build());
                log.info("MinIO Bucket [{}] created", properties.getBucket());
            }
            log.info("MinIO Client initialized: endpoint={}, bucket={}",
                properties.getEndpoint(), properties.getBucket());
        } catch (Exception e) {
            log.error("MinIO Bucket initialization failed: {}", e.getMessage(), e);
        }

        return client;
    }

    /**
     * 创建 MinioUtils Bean。
     * <p>
     * 仅当 MinioClient Bean 存在时才创建。
     *
     * @param minioClient MinioClient 实例
     * @param properties  MinIO 配置属性
     * @return MinioUtils 实例
     */
    @Bean
    @ConditionalOnProperty(prefix = "sca.minio", name = "endpoint")
    public MinioUtils minioUtils(MinioClient minioClient, MinioProperties properties) {
        return new MinioUtils(minioClient, properties);
    }
}
