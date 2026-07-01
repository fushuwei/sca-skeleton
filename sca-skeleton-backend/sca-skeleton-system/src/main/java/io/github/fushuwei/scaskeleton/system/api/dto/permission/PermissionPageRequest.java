package io.github.fushuwei.scaskeleton.system.api.dto.permission;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.Set;

/**
 * 权限（菜单）分页查询请求 DTO。
 *
 * @author Fu Wei
 */
@Data
public class PermissionPageRequest {

    /** 允许排序的字段白名单，防止 SQL 注入 */
    private static final Set<String> ALLOWED_ORDER_FIELDS = Set.of(
            "name", "code", "type", "sort", "status", "create_time"
    );

    /** 页码，从 1 开始 */
    @Min(value = 1, message = "页码不能小于 1")
    private Integer pageNum = 1;

    /** 每页条数，上限 100 防止全表拉取 */
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private Integer pageSize = 20;

    /** 父节点 ID，顶级为 "0" */
    private String parentId;

    /** 综合搜索关键词（名称、权限标识模糊匹配） */
    private String keyword;

    /** 权限类型筛选：folder / menu / button */
    private String type;

    /** 状态筛选：enabled / disabled */
    private String status;

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

    /** 返回安全的排序方向，默认 asc（菜单按排序号升序更合理） */
    public String safeOrderDirection() {
        if ("desc".equalsIgnoreCase(orderDirection)) {
            return "DESC";
        }
        return "ASC";
    }
}
