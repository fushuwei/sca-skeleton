package io.github.fushuwei.scaskeleton.datasource.api.request.query;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * SQL 执行请求
 *
 * @author Fu Wei
 */
@Data
public class SqlExecuteRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "数据源 ID 不能为空")
    private String datasourceId;

    @NotBlank(message = "SQL 语句不能为空")
    private String sql;

    /** 目标数据库（可选，部分数据库支持跨库查询） */
    private String database;

    /** 最大返回行数，默认 1000，上限 10000 */
    @PositiveOrZero(message = "最大行数不能为负数")
    private Integer maxRows = 1000;
}
