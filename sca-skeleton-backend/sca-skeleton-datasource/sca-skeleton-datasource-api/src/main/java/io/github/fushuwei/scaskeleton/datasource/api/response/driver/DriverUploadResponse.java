package io.github.fushuwei.scaskeleton.datasource.api.response.driver;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 驱动文件上传响应。
 * <p>
 * 支持多文件上传：上传的驱动文件会临时存到 drivers/_temp/{uploadId}/ 目录，
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

    /** 所有驱动文件总大小（字节） */
    private Long fileSize;

    /** 上传的驱动文件名列表 */
    private List<String> jarFileNames;

    /** 探测到的驱动类候选列表 */
    private List<String> detectedDriverClasses;

    /** 自动选择的第一个驱动类（探测为空则为 null） */
    private String driverClass;
}
