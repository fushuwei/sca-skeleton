package io.github.fushuwei.scaskeleton.datasource.engine.storage;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * 驱动 JAR 存储接口。
 * <p>
 * 采用 Chat2DB 风格的「目录式管理」：每个驱动记录对应一个目录，目录下存放驱动主 JAR + 所有依赖 JAR。
 * <p>
 * 对象键格式：{driverName}/{fileName}.jar
 * <ul>
 *   <li>driverName：驱动记录的名称（唯一），作为目录名</li>
 *   <li>fileName：上传时的原始文件名</li>
 * </ul>
 * 本地存储完整路径：{basePath}/{driverName}/{fileName}.jar
 * <p>
 * 同一驱动目录下所有 JAR 共同构成「自包含依赖环境」，加载时用一个 {@link java.net.URLClassLoader} 全部加载，
 * 通过标准 parent-first 双亲委派实现类隔离，无需 child-first 反转。
 *
 * @author Fu Wei
 */
public interface DriverStore {

    /**
     * 存储单个 JAR 文件到指定驱动目录。
     *
     * @param driverDir   驱动目录键（{driverName}）
     * @param fileName    JAR 文件名
     * @param inputStream JAR 文件输入流
     * @param fileSize    文件大小（字节）
     */
    void putJar(String driverDir, String fileName, InputStream inputStream, long fileSize);

    /**
     * 检查指定 JAR 是否存在。
     *
     * @param driverDir 驱动目录键
     * @param fileName  JAR 文件名
     * @return 存在返回 true
     */
    boolean jarExists(String driverDir, String fileName);

    /**
     * 列出驱动目录下所有 JAR 文件的本地路径。
     * <p>
     * LocalDriverStore 直接返回本地文件系统路径；MinioDriverStore 会先将所有 JAR 下载到本地缓存目录再返回。
     *
     * @param driverDir 驱动目录键
     * @return 本地 JAR 文件路径列表
     */
    List<Path> listLocalJars(String driverDir);

    /**
     * 删除整个驱动目录（含目录下所有 JAR）。
     *
     * @param driverDir 驱动目录键
     */
    void deleteDriverDir(String driverDir);

    /**
     * 重命名驱动目录（修改驱动名称时调用）。
     *
     * @param oldDriverDir 旧驱动目录键
     * @param newDriverDir 新驱动目录键
     */
    void renameDriverDir(String oldDriverDir, String newDriverDir);

    /**
     * 删除驱动目录下的单个 JAR 文件。
     *
     * @param driverDir 驱动目录键
     * @param fileName  JAR 文件名
     */
    void deleteJar(String driverDir, String fileName);
}
