package io.github.fushuwei.scaskeleton.datasource.api.response.driver;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 驱动响应
 *
 * @author Fu Wei
 */
@Data
public class DriverResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String dbType;
    private String driverName;
    private String driverClass;
    private String objectKey;
    private String storageType;
    private String urlTemplate;
    private String remark;
    private Integer version;
    private LocalDateTime createTime;

    /** 驱动文件列表（一个驱动可包含多个文件） */
    private List<DriverFileResponse> files;

    /** 所有文件总大小（字节，聚合展示用） */
    private Long totalFileSize;
}
