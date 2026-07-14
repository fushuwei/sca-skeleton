package io.github.fushuwei.scaskeleton.system.api.request.operationlog;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 操作日志分页查询请求对象
 *
 * @author Fu Wei
 */
@Data
public class OperationLogPageRequest {

    /** 允许排序的字段白名单 */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "operation_time", "operator", "module", "action", "http_method", "request_uri", "client_ip", "cost_ms", "is_success"
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

    /** 操作模块 */
    private String module;

    /** 操作动作 */
    private String action;

    /** 操作人 */
    private String operator;

    /** 是否成功 */
    private Integer isSuccess;

    /** 查询开始时间 */
    private LocalDateTime startTime;

    /** 查询结束时间 */
    private LocalDateTime endTime;

    // ==================== 排序参数 ====================

    /** 排序字段 */
    private String sortField;

    /** 排序方向 */
    private String sortOrder;

    /** 返回安全的排序字段（不在白名单则返回 null） */
    public String safeSortField() {
        if (sortField != null && ALLOWED_SORT_FIELDS.contains(sortField)) {
            return sortField;
        }
        return null;
    }

    /** 返回安全的排序方向（默认 asc） */
    public String safeSortOrder() {
        if ("desc".equalsIgnoreCase(sortOrder)) {
            return "DESC";
        }
        return "ASC";
    }
}
