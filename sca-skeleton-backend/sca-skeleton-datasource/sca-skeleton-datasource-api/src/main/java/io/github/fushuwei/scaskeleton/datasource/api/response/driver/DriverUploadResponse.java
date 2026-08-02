package io.github.fushuwei.scaskeleton.datasource.api.response.driver;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 驱动 JAR 上传响应。
 * <p>
 * 上传后自动探测驱动类，前端拿到 sha256/objectKey/driverClass 后回填到创建表单。
 *
 * @author Fu Wei
 */
@Data
public class DriverUploadResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** JAR 文件 SHA256 */
    private String jarSha256;

    /** 内容寻址对象键（driver/{dbType}/{sha256}/{name}.jar） */
    private String objectKey;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 探测到的驱动类候选列表 */
    private List<String> detectedDriverClasses;

    /** 自动选择的第一个驱动类（探测为空则为 null） */
    private String driverClass;
}
