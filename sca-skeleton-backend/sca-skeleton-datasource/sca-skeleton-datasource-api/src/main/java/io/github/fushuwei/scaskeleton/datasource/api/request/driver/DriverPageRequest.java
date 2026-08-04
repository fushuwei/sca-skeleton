package io.github.fushuwei.scaskeleton.datasource.api.request.driver;

import io.github.fushuwei.scaskeleton.datasource.api.enums.DbType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 驱动分页查询请求
 *
 * @author Fu Wei
 */
@Data
public class DriverPageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 数据库类型（可选筛选） */
    private DbType dbType;

    /** 关键字搜索（驱动名称/类名） */
    private String keyword;

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 10;
}
