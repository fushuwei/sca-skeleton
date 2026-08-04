package io.github.fushuwei.scaskeleton.datasource.engine.storage;

import io.github.fushuwei.scaskeleton.minio.util.MinioUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * MinIO 驱动存储实现。
 * <p>
 * 采用目录式管理：每个驱动记录对应一个 MinIO 前缀（虚拟目录），其下存放驱动主 JAR + 所有依赖 JAR。
 * 对象键格式：{name}/{fileName}.jar
 * <p>
 * 加载时将目录下所有 JAR 下载到本地临时目录，再用 {@link java.net.URLClassLoader} 加载。
 * <p>
 * 注意：当前 {@link MinioUtils} 未提供 listObjects 方法，本实现暂不支持 listLocalJars 和 renameDriverDir。
 * 生产启用 MinIO 时需先在 MinioUtils 补充 listObjects 能力，或改为「在 ds_driver_file 表
 * 中存储每个 JAR 的 objectKey」方案。
 *
 * @author Fu Wei
 */
@Slf4j
@RequiredArgsConstructor
public class MinioDriverStore implements DriverStore {

    private final MinioUtils minioUtils;

    @Override
    public void putJar(String driverDir, String fileName, InputStream inputStream, long fileSize) {
        String objectKey = driverDir + "/" + fileName;
        minioUtils.putObject(objectKey, inputStream, fileSize, "application/java-archive");
    }

    @Override
    public boolean jarExists(String driverDir, String fileName) {
        String objectKey = driverDir + "/" + fileName;
        return minioUtils.exists(objectKey);
    }

    @Override
    public List<Path> listLocalJars(String driverDir) {
        // MinioUtils 暂未提供 listObjects，需要补充。
        // 当前实现：依赖驱动记录中保存的 jarList（JSON 数组），由调用方传入每个 JAR 的 objectKey 下载。
        // 这里保留接口实现，待 MinioUtils 补充 listObjects 后改为扫描 driverDir/ 前缀下所有对象。
        throw new UnsupportedOperationException(
            "MinioDriverStore.listLocalJars 暂未实现，需先在 MinioUtils 补充 listObjects 能力");
    }

    @Override
    public void deleteDriverDir(String driverDir) {
        // 同上，需要 listObjects 才能遍历删除目录下所有对象。
        // 当前实现仅删除目录前缀（MinIO 无真实目录概念，此操作实际为空）。
        // 实际删除由 DriverServiceImpl 在驱动记录中保存 jarList 后逐个调用 deleteObject 完成。
        log.warn("MinioDriverStore.deleteDriverDir 需 MinioUtils.listObjects 支持，当前为空操作: {}", driverDir);
    }

    @Override
    public void renameDriverDir(String oldDriverDir, String newDriverDir) {
        // 同 listLocalJars，需要 listObjects 才能遍历复制 + 删除旧对象。
        throw new UnsupportedOperationException(
            "MinioDriverStore.renameDriverDir 暂未实现，需先在 MinioUtils 补充 listObjects 能力");
    }

    @Override
    public void deleteJar(String driverDir, String fileName) {
        String objectKey = driverDir + "/" + fileName;
        minioUtils.deleteObject(objectKey);
    }

    @Override
    public Path getLocalJarPath(String driverDir, String fileName) {
        // 需要先下载到本地临时目录，待 MinioUtils 补充 getObject 能力后实现。
        throw new UnsupportedOperationException(
            "MinioDriverStore.getLocalJarPath 暂未实现，需先在 MinioUtils 补充 getObject 能力");
    }
}
