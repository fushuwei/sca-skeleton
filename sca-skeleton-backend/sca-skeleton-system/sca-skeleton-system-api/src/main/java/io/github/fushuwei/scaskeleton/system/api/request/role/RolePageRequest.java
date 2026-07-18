package io.github.fushuwei.scaskeleton.system.api.request.role;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.Map;

/**
 * 角色分页查询请求对象
 *
 * @author Fu Wei
 */
@Data
public class RolePageRequest {

    /** 允许排序的字段白名单 */
    private static final Map<String, String> ALLOWED_SORT_FIELD_MAP = Map.of(
        "name", "r.name",
        "code", "r.code",
        "data_scope", "r.data_scope",
        "sort", "r.sort",
        "create_time", "r.create_time",
        "permission_count", "permission_count"
    );

    // ==================== 分页参数 ====================

    /** 页码（从 1 开始） */
    @Min(value = 1, message = "页码不能小于 1")
    private Integer pageNum = 1;

    /** 每页条数 */
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private Integer pageSize = 20;

    // ==================== 查询条件 ====================

    /** 综合搜索关键词 */
    private String keyword;

    /** 数据权限范围 */
    private String dataScope;

    // ==================== 排序参数 ====================

    /** 排序字段 */
    private String sortField;

    /** 排序方向 */
    private String sortOrder;

    /** 返回安全的排序字段 SQL 表达式（不在白名单则返回 null） */
    public String safeSortField() {
        return sortField == null ? null : ALLOWED_SORT_FIELD_MAP.get(sortField);
    }

    /** 返回安全的排序方向（默认 asc） */
    public String safeSortOrder() {
        if ("desc".equalsIgnoreCase(sortOrder)) {
            return "DESC";
        }
        return "ASC";
    }
}
