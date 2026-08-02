package io.github.fushuwei.scaskeleton.minio.util;

import io.github.fushuwei.scaskeleton.minio.config.MinioProperties;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * MinIO 工具类，封装常用的对象存储操作。
 * <p>
 * 通过 {@link io.github.fushuwei.scaskeleton.minio.config.MinioAutoConfiguration} 的 @Bean 创建，
 * 不使用 @Component，避免 MinioClient 不存在时启动报错。
 *
 * @author Fu Wei
 */
@Slf4j
@RequiredArgsConstructor
public class MinioUtils {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    /**
     * 上传对象
     */
    public void putObject(String objectKey, InputStream inputStream, long size, String contentType) {
        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .stream(inputStream, size, -1)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO 上传对象失败: " + objectKey, e);
        }
    }

    /**
     * 获取对象输入流
     */
    public InputStream getObject(String objectKey) {
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO 获取对象失败: " + objectKey, e);
        }
    }

    /**
     * 检查对象是否存在
     */
    public boolean exists(String objectKey) {
        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 下载对象到本地路径
     */
    public Path downloadToLocal(String objectKey) {
        try (InputStream is = getObject(objectKey)) {
            Path tempPath = Files.createTempFile("minio-dl-", ".tmp");
            Files.copy(is, tempPath, StandardCopyOption.REPLACE_EXISTING);
            return tempPath;
        } catch (Exception e) {
            throw new RuntimeException("MinIO 下载对象失败: " + objectKey, e);
        }
    }

    /**
     * 删除对象
     */
    public void deleteObject(String objectKey) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.warn("MinIO 删除对象失败: {} - {}", objectKey, e.getMessage());
        }
    }

    /**
     * 生成预签名下载 URL（M3 导出功能使用，届时实现）
     */
    public String getPresignedUrl(String objectKey, int expiry) {
        throw new UnsupportedOperationException("预签名 URL 功能在 M3 阶段实现");
    }
}
