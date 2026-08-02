package io.github.fushuwei.scaskeleton.datasource.engine.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地文件系统驱动存储实现。
 * <p>
 * 首期（M0/M1）使用，零基础设施依赖。
 * 存储路径由配置项 sca.datasource.driver.base-path 决定，默认 {@code ~/.sca-skeleton/drivers}。
 * 本地缓存按 sha256 作键（{@code <basePath>/cache/{sha256}.jar}）。
 *
 * @author Fu Wei
 */
public class LocalDriverStore implements DriverStore {

    private final Path basePath;

    /**
     * 构造本地驱动存储。
     *
     * @param basePath 本地存储根路径
     */
    public LocalDriverStore(String basePath) {
        this.basePath = Paths.get(basePath);
    }

    @Override
    public void putObject(String objectKey, InputStream inputStream, long fileSize) {
        try {
            Path targetPath = resolveObjectPath(objectKey);
            Files.createDirectories(targetPath.getParent());
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("存储驱动 JAR 失败: " + objectKey, e);
        }
    }

    @Override
    public InputStream getObject(String objectKey) {
        try {
            Path filePath = resolveObjectPath(objectKey);
            if (!Files.exists(filePath)) {
                throw new RuntimeException("驱动 JAR 不存在: " + objectKey);
            }
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            throw new RuntimeException("读取驱动 JAR 失败: " + objectKey, e);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        return Files.exists(resolveObjectPath(objectKey));
    }

    @Override
    public Path downloadToLocal(String objectKey) {
        Path filePath = resolveObjectPath(objectKey);
        if (!Files.exists(filePath)) {
            throw new RuntimeException("驱动 JAR 不存在: " + objectKey);
        }
        return filePath;
    }

    @Override
    public void deleteObject(String objectKey) {
        try {
            Files.deleteIfExists(resolveObjectPath(objectKey));
        } catch (IOException e) {
            throw new RuntimeException("删除驱动 JAR 失败: " + objectKey, e);
        }
    }

    /**
     * 将对象键解析为本地文件路径。
     * <p>
     * 对象键格式：driver/{dbType}/{sha256}/{name}.jar
     * 本地路径：{basePath}/driver/{dbType}/{sha256}/{name}.jar
     *
     * @param objectKey 内容寻址对象键
     * @return 本地文件路径
     */
    private Path resolveObjectPath(String objectKey) {
        return basePath.resolve(objectKey);
    }

    /**
     * 获取本地存储根路径。
     *
     * @return 根路径
     */
    public Path getBasePath() {
        return basePath;
    }
}
