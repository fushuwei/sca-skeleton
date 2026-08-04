package io.github.fushuwei.scaskeleton.datasource.api.request.driver;

import io.github.fushuwei.scaskeleton.datasource.api.enums.DbType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 驱动新增请求
 * <p>
 * 表单字段与驱动文件随同一次 multipart 请求提交（本对象为 JSON 部分 "driver"，
 * 文件部分为 "files"）。创建时用 driverName 作为正式目录名，文件直接写入 {basePath}/{driverName}/。
 *
 * @author Fu Wei
 */
@Data
public class DriverCreateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "驱动名称不能为空")
    private String driverName;

    @NotNull(message = "数据库类型不能为空")
    private DbType dbType;

    @NotBlank(message = "驱动类名不能为空")
    private String driverClass;

    /** JDBC URL 模板 */
    private String urlTemplate;

    /** URL 参数白名单（JSON） */
    private String allowedParams;

    /** 备注 */
    private String remark;
}
