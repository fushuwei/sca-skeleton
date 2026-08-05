package io.github.fushuwei.scaskeleton.datasource.api.request.datasource;

import io.github.fushuwei.scaskeleton.datasource.api.enums.DbType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数据源更新请求
 * <p>
 * password 为空表示不修改密码，非空才更新。
 *
 * @author Fu Wei
 */
@Data
public class DatasourceUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "ID 不能为空")
    private String id;

    private String name;
    private DbType dbType;
    private String driverId;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;

    /** 密码为空表示不修改 */
    private String password;

    private String connectionParams;
    private String poolConfig;

    /** 备注 */
    private String remark;

    /** 乐观锁版本号 */
    private Integer version;
}
