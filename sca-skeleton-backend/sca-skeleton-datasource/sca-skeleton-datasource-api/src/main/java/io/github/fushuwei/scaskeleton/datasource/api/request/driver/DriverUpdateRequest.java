package io.github.fushuwei.scaskeleton.datasource.api.request.driver;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 驱动更新请求
 *
 * @author Fu Wei
 */
@Data
public class DriverUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "ID 不能为空")
    private String id;

    /** 驱动名称 */
    private String driverName;

    /** JDBC URL 模板 */
    private String urlTemplate;

    /** URL 参数白名单（JSON） */
    private String allowedParams;

    /** 备注 */
    private String remark;

    /** 乐观锁版本号 */
    private Integer version;
}
