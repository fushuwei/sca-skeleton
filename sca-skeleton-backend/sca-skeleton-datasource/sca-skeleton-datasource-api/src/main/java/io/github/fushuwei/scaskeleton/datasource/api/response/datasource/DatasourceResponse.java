package io.github.fushuwei.scaskeleton.datasource.api.response.datasource;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据源响应
 * <p>
 * password 字段不返回真实密文（返回 null/掩码）。
 *
 * @author Fu Wei
 */
@Data
public class DatasourceResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String tenantId;
    private String name;
    private String dbType;
    private String driverId;
    private String driverName;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    /** 密码不返回真实密文 */
    private String password;
    private String connectionParams;
    private String poolConfig;
    private Integer isEnabled;
    private String status;
    private String errorMsg;
    private String remark;
    private Integer version;
    private LocalDateTime createTime;
}
