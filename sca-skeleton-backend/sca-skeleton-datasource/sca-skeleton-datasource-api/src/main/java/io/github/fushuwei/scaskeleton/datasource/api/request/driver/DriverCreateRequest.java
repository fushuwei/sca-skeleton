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
 * 前端先调用 /driver/upload 上传 JAR（支持多文件），拿到 uploadId 后回填到此请求。
 * 创建时用 driverName 作为正式目录名，把临时目录 drivers/_temp/{uploadId}/ 重命名为 drivers/{driverName}/。
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

    /** 上传批次 ID（上传 JAR 后返回，用于定位临时目录） */
    @NotBlank(message = "请先上传驱动 JAR 文件")
    private String uploadId;

    /** JDBC URL 模板 */
    private String urlTemplate;

    /** URL 参数白名单（JSON） */
    private String allowedParams;

    /** 备注 */
    private String remark;
}
