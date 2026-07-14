package io.github.fushuwei.scaskeleton.system.api.request.role;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.Set;

/**
 * 角色分页查询请求对象
 *
 * @author Fu Wei
 */
@Data
public class RolePageRequest {

    /** 允许排序的字段白名单 */
    private static final Set<String> ALLOWED_ORDER_FIELDS = Set.of(
        "name", "code", "data_scope", "sort", "create_time", "permission_count"
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
    private String orderBy;

    /** 排序方向 */
    private String orderDirection;

    /** 返回安全的排序字段（不在白名单则返回 null） */
    public String safeOrderBy() {
        if (orderBy != null && ALLOWED_ORDER_FIELDS.contains(orderBy)) {
            return orderBy;
        }
        return null;
    }

    /** 返回安全的排序方向（默认 asc） */
    public String safeOrderDirection() {
        if ("desc".equalsIgnoreCase(orderDirection)) {
            return "DESC";
        }
        return "ASC";
    }
}
