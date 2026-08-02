package io.github.fushuwei.scaskeleton.datasource.api.response.driver;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

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
    private String driverVersion;
    private String jarSha256;
    private String objectKey;
    private Long fileSize;
    private String storageType;
    private String urlTemplate;
    private String allowedParams;
    private String status;
    private Integer isBuiltin;
    private String remark;
    private String createTime;
}
