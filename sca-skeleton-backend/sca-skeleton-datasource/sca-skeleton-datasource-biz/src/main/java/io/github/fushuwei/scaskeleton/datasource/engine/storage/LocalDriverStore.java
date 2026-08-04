package io.github.fushuwei.scaskeleton.datasource.engine.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 本地文件系统驱动存储实现。
 * <p>
 * 采用目录式管理：每个驱动记录对应一个本地目录，目录下存放驱动主 JAR + 所有依赖 JAR。
 * 存储路径：{basePath}/{name}/{fileName}.jar
 *
 * @author Fu Wei
 */
public class LocalDriverStore implements DriverStore {

    private final Path basePath;

    public LocalDriverStore(String basePath) {
        this.basePath = Paths.get(basePath);
    }

    @Override
    public void putJar(String driverDir, String fileName, InputStream inputStream, long fileSize) {
        try {
            Path targetPath = resolveJarPath(driverDir, fileName);
            Files.createDirectories(targetPath.getParent());
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("存储驱动 JAR 失败: " + driverDir + "/" + fileName, e);
        }
    }

    @Override
    public boolean jarExists(String driverDir, String fileName) {
        return Files.exists(resolveJarPath(driverDir, fileName));
    }

    @Override
    public List<Path> listLocalJars(String driverDir) {
        Path dirPath = resolveDriverDirPath(driverDir);
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            throw new RuntimeException("驱动目录不存在: " + driverDir);
        }
        List<Path> jars = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dirPath)) {
            stream.filter(p -> p.toString().endsWith(".jar"))
                .forEach(jars::add);
        } catch (IOException e) {
            throw new RuntimeException("列出驱动 JAR 失败: " + driverDir, e);
        }
        if (jars.isEmpty()) {
            throw new RuntimeException("驱动目录下无 JAR 文件: " + driverDir);
        }
        return jars;
    }

    @Override
    public void deleteDriverDir(String driverDir) {
        Path dirPath = resolveDriverDirPath(driverDir);
        if (!Files.exists(dirPath)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dirPath)) {
            walk.sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                        // 忽略单个文件删除失败
                    }
                });
        } catch (IOException e) {
            throw new RuntimeException("删除驱动目录失败: " + driverDir, e);
        }
    }

    @Override
    public void renameDriverDir(String oldDriverDir, String newDriverDir) {
        Path oldPath = resolveDriverDirPath(oldDriverDir);
        Path newPath = resolveDriverDirPath(newDriverDir);
        if (!Files.exists(oldPath)) {
            throw new RuntimeException("源驱动目录不存在: " + oldDriverDir);
        }
        try {
            Files.createDirectories(newPath.getParent());
            Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("重命名驱动目录失败: " + oldDriverDir + " -> " + newDriverDir, e);
        }
    }

    @Override
    public void deleteJar(String driverDir, String fileName) {
        Path jarPath = resolveJarPath(driverDir, fileName);
        try {
            Files.deleteIfExists(jarPath);
        } catch (IOException e) {
            throw new RuntimeException("删除驱动 JAR 失败: " + driverDir + "/" + fileName, e);
        }
    }

    @Override
    public Path getLocalJarPath(String driverDir, String fileName) {
        return resolveJarPath(driverDir, fileName);
    }

    /**
     * 解析驱动目录的完整本地路径。
     * <p>
     * driverDir 格式：{name}
     * 本地路径：{basePath}/{name}
     */
    private Path resolveDriverDirPath(String driverDir) {
        return basePath.resolve(driverDir);
    }

    /**
     * 解析单个 JAR 文件的完整本地路径。
     */
    private Path resolveJarPath(String driverDir, String fileName) {
        return resolveDriverDirPath(driverDir).resolve(fileName);
    }

    public Path getBasePath() {
        return basePath;
    }
}
