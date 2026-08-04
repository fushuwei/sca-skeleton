package io.github.fushuwei.scaskeleton.datasource.api.request.driver;

import io.github.fushuwei.scaskeleton.datasource.api.enums.DbType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 驱动分页查询请求
 *
 * @author Fu Wei
 */
@Data
public class DriverPageRequest implements Serializable {

    /**
     * 允许排序的字段白名单
     * <p>
     * value 为安全的 SQL 列名或别名（与 DriverMapper.xml 中的 SELECT 别名对应）：
     * - 直接字段：列名（如 {@code name}）
     * - 计算字段：LEFT JOIN 子查询中定义的别名（如 {@code total_file_size}）
     */
    private static final Map<String, String> ALLOWED_SORT_FIELD_MAP = Map.of(
        "name", "name",
        "db_type", "db_type",
        "driver_class", "driver_class",
        "create_time", "create_time",
        "total_file_size", "total_file_size"
    );

    /** 数据库类型（可选筛选） */
    private DbType dbType;

    /** 关键字搜索（驱动名称/类名） */
    private String keyword;

    /** 页码（从 1 开始） */
    @Min(value = 1, message = "页码不能小于 1")
    private Integer pageNum = 1;

    /** 每页条数 */
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private Integer pageSize = 10;

    // ==================== 排序参数 ====================

    /** 排序字段 */
    private String sortField;

    /** 排序方向 */
    private String sortOrder;

    /** 返回安全的排序字段 SQL 表达式（不在白名单则返回 null） */
    public String safeSortField() {
        return sortField == null ? null : ALLOWED_SORT_FIELD_MAP.get(sortField);
    }

    /** 返回安全的排序方向（默认 desc） */
    public String safeSortOrder() {
        if ("asc".equalsIgnoreCase(sortOrder)) {
            return "ASC";
        }
        return "DESC";
    }
}
