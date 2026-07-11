package io.github.fushuwei.scaskeleton.system.api.request.operationlog;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 操作日志分页查询请求对象。
 *
 * @author Fu Wei
 */
@Data
public class OperationLogPageRequest {

    /** 允许排序的字段白名单，防止 SQL 注入 */
    private static final Set<String> ALLOWED_ORDER_FIELDS = Set.of(
            "operation_time", "username", "module", "action",
            "http_method", "request_uri", "client_ip", "cost_ms", "success"
    );

    /** 页码，从 1 开始 */
    @Min(value = 1, message = "页码不能小于 1")
    private Integer pageNum = 1;

    /** 每页条数，上限 100 防止全表拉取 */
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private Integer pageSize = 20;

    /** 操作模块筛选 */
    private String module;

    /** 操作动作筛选 */
    private String action;

    /** 操作人用户名筛选 */
    private String username;

    /** 操作状态筛选：true-成功，false-失败 */
    private Boolean success;

    /** 查询开始时间 */
    private LocalDateTime startTime;

    /** 查询结束时间 */
    private LocalDateTime endTime;

    /** 排序字段 */
    private String orderBy;

    /** 排序方向：asc / desc */
    private String orderDirection;

    /** 校验并返回安全的排序字段名，不在白名单内则返回 null */
    public String safeOrderBy() {
        if (orderBy != null && ALLOWED_ORDER_FIELDS.contains(orderBy)) {
            return orderBy;
        }
        return null;
    }

    /** 返回安全的排序方向，默认 asc */
    public String safeOrderDirection() {
        if ("desc".equalsIgnoreCase(orderDirection)) {
            return "DESC";
        }
        return "ASC";
    }
}
