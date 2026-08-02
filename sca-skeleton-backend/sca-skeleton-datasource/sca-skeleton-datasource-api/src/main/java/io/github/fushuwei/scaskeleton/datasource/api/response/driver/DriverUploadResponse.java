package io.github.fushuwei.scaskeleton.datasource.api.response.driver;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 驱动 JAR 上传响应。
 * <p>
 * 支持多 JAR 上传：上传的 JAR 会临时存到 drivers/_temp/{uploadId}/ 目录，
 * 前端拿到 uploadId + 探测到的 driverClass 后回填到创建表单。
 * 创建驱动时用 driverName 作为正式目录名，把临时目录重命名为 drivers/{driverName}/。
 *
 * @author Fu Wei
 */
@Data
public class DriverUploadResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 上传批次 ID（临时目录 drivers/_temp/{uploadId}/ 的标识） */
    private String uploadId;

    /** 主 JAR 文件 SHA256（多 JAR 时为首个 JAR 的 SHA256，用于展示） */
    private String jarSha256;

    /** 所有 JAR 文件总大小（字节） */
    private Long fileSize;

    /** 上传的 JAR 文件名列表 */
    private List<String> jarFileNames;

    /** 探测到的驱动类候选列表 */
    private List<String> detectedDriverClasses;

    /** 自动选择的第一个驱动类（探测为空则为 null） */
    private String driverClass;
}
