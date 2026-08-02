package io.github.fushuwei.scaskeleton.datasource.api.request.driver;

import io.github.fushuwei.scaskeleton.datasource.api.enums.DbType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 驱动新增请求
 *
 * @author Fu Wei
 */
@Data
public class DriverCreateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "数据库类型不能为空")
    private DbType dbType;

    @NotBlank(message = "驱动名称不能为空")
    private String driverName;

    @NotBlank(message = "驱动类名不能为空")
    private String driverClass;

    @NotBlank(message = "驱动版本不能为空")
    private String driverVersion;

    /** JAR SHA256（上传后回填） */
    private String jarSha256;

    /** 存储对象键 */
    private String objectKey;

    /** 文件大小（字节） */
    private Long fileSize;

    /** JDBC URL 模板 */
    private String urlTemplate;

    /** URL 参数白名单（JSON） */
    private String allowedParams;

    /** 备注 */
    private String remark;
}
