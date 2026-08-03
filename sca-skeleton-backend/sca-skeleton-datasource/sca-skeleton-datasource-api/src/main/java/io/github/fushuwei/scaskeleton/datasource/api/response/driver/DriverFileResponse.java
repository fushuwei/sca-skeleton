package io.github.fushuwei.scaskeleton.datasource.api.response.driver;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 驱动文件响应（单个驱动文件的元数据）
 *
 * @author Fu Wei
 */
@Data
public class DriverFileResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 文件名 */
    private String fileName;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件 SHA256 校验值 */
    private String sha256;

    /** 排序序号（主文件 0） */
    private Integer sortOrder;
}