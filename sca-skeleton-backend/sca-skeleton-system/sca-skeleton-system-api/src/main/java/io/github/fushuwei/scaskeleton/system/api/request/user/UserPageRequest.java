package io.github.fushuwei.scaskeleton.system.api.request.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.Map;

/**
 * 用户分页查询请求对象
 *
 * @author Fu Wei
 */
@Data
public class UserPageRequest {

    /** 允许排序的字段白名单 */
    private static final Map<String, String> ALLOWED_SORT_FIELD_MAP = Map.of(
        "username", "u.username",
        "nickname", "u.nickname",
        "real_name", "u.real_name",
        "realm", "u.realm",
        "is_superadmin", "u.is_superadmin",
        "status", "u.status",
        "create_time", "u.create_time",
        "dept_name", "d.dept_names"
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

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 用户域（admin：后台用户；portal：前台用户） */
    private String realm;

    /** 用户状态 */
    private String status;

    /** 部门 ID */
    private String deptId;

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
