package io.github.fushuwei.scaskeleton.datasource.api.response.query;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * SQL 执行响应
 *
 * @author Fu Wei
 */
@Data
public class SqlExecuteResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 列名列表 */
    private List<String> columns;

    /** 数据行列表，每行为 列名 → 值 的 Map */
    private List<Map<String, Object>> rows;

    /** 实际返回行数 */
    private Integer rowCount;

    /** 执行耗时（毫秒） */
    private Long costMs;
}
