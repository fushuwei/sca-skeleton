package io.github.fushuwei.scaskeleton.datasource.engine.storage;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * 驱动 JAR 存储接口。
 * <p>
 * 抽象存储层，支持本地文件系统与 MinIO 对象存储两种实现。
 * 首期（M0/M1）使用 {@link LocalDriverStore}，MinIO 在 M3/M4 引入。
 * <p>
 * 对象键采用内容寻址：driver/{dbType}/{sha256}/{originalName}.jar，保证不可变。
 *
 * @author Fu Wei
 */
public interface DriverStore {

    /**
     * 存储驱动 JAR 文件。
     *
     * @param objectKey   内容寻址对象键（driver/{dbType}/{sha256}/{name}.jar）
     * @param inputStream JAR 文件输入流
     * @param fileSize    文件大小（字节）
     */
    void putObject(String objectKey, InputStream inputStream, long fileSize);

    /**
     * 获取驱动 JAR 文件输入流。
     *
     * @param objectKey 内容寻址对象键
     * @return JAR 文件输入流
     */
    InputStream getObject(String objectKey);

    /**
     * 检查对象是否存在。
     *
     * @param objectKey 内容寻址对象键
     * @return 存在返回 true
     */
    boolean exists(String objectKey);

    /**
     * 将远程对象下载到本地缓存路径。
     *
     * @param objectKey 内容寻址对象键
     * @return 本地文件路径
     */
    Path downloadToLocal(String objectKey);

    /**
     * 删除对象。
     *
     * @param objectKey 内容寻址对象键
     */
    void deleteObject(String objectKey);
}
