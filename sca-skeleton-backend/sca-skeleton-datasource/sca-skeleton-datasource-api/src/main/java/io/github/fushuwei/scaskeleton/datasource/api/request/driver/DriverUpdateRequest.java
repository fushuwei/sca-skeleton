package io.github.fushuwei.scaskeleton.datasource.api.request.driver;

import io.github.fushuwei.scaskeleton.datasource.api.enums.DbType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

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
    private String name;

    /** 数据库类型 */
    private DbType dbType;

    /** JDBC Driver 全限定类名（MongoDB 等非 JDBC 类型可为空） */
    private String driverClass;

    /** JDBC URL 模板 */
    private String urlTemplate;

    /** 备注 */
    private String remark;

    /** 乐观锁版本号 */
    private Integer version;

    /** 编辑时删除的已有文件名列表 */
    private List<String> deletedFileNames;
}
