package io.github.fushuwei.scaskeleton.datasource.api.request.datasource;

import io.github.fushuwei.scaskeleton.datasource.api.enums.DbType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 数据源新增请求
 *
 * @author Fu Wei
 */
@Data
public class DatasourceCreateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "数据源名称不能为空")
    private String name;

    @NotNull(message = "数据库类型不能为空")
    private DbType dbType;

    private String driverId;

    @NotBlank(message = "主机地址不能为空")
    private String host;

    @NotNull(message = "端口不能为空")
    private Integer port;

    private String databaseName;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** JDBC 连接参数（JSON） */
    private String connectionParams;

    /** 连接池配置（结构化 key/value，后端白名单校验后序列化为 JSON 存储） */
    private Map<String, Object> poolConfig;

    /** 备注 */
    private String remark;
}
