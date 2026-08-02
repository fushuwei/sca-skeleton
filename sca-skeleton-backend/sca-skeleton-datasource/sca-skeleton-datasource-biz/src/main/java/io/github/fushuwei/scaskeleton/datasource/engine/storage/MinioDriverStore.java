package io.github.fushuwei.scaskeleton.datasource.engine.storage;

import io.github.fushuwei.scaskeleton.minio.util.MinioUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * MinIO 驱动存储实现。
 * <p>
 * 生产环境使用，解决分布式高可用场景下的驱动 JAR 共享问题。
 * 对象键采用内容寻址：driver/{dbType}/{sha256}/{name}.jar，保证不可变。
 * 本地缓存按 sha256 作键，跨节点一致性由内容寻址保证。
 *
 * @author Fu Wei
 */
@Slf4j
@RequiredArgsConstructor
public class MinioDriverStore implements DriverStore {

    private final MinioUtils minioUtils;

    @Override
    public void putObject(String objectKey, InputStream inputStream, long fileSize) {
        minioUtils.putObject(objectKey, inputStream, fileSize, "application/java-archive");
    }

    @Override
    public InputStream getObject(String objectKey) {
        return minioUtils.getObject(objectKey);
    }

    @Override
    public boolean exists(String objectKey) {
        return minioUtils.exists(objectKey);
    }

    @Override
    public Path downloadToLocal(String objectKey) {
        return minioUtils.downloadToLocal(objectKey);
    }

    @Override
    public void deleteObject(String objectKey) {
        minioUtils.deleteObject(objectKey);
    }
}
