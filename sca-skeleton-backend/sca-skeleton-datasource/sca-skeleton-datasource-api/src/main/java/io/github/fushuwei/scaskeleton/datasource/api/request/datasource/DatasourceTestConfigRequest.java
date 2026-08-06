package io.github.fushuwei.scaskeleton.datasource.api.request.datasource;

import io.github.fushuwei.scaskeleton.datasource.api.enums.DbType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数据源连接配置测试请求（表单未保存时的「测试连接」）。
 * <p>
 * 与创建/编辑表单的提交体对齐：编辑模式下密码可留空，
 * 后端将回退使用该数据源已入库的密码完成测试。
 *
 * @author Fu Wei
 */
@Data
public class DatasourceTestConfigRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 数据源 ID（编辑模式传入，用于回退库内密码；新增模式为空） */
    private String id;

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

    /** 密码（编辑模式留空时使用库内已保存的密码） */
    private String password;

    /** JDBC 连接参数（JSON 或 query string） */
    private String connectionParams;
}
