package io.github.fushuwei.scaskeleton.system.api.request.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.Set;

/**
 * 用户分页查询请求对象。
 *
 * @author Fu Wei
 */
@Data
public class UserPageRequest {

    /** 允许排序的字段白名单，防止 SQL 注入 */
    private static final Set<String> ALLOWED_ORDER_FIELDS = Set.of(
            "username", "nickname", "real_name", "user_type", "status", "create_time", "dept_name"
    );

    /** 页码，从 1 开始 */
    @Min(value = 1, message = "页码不能小于 1")
    private Integer pageNum = 1;

    /** 每页条数，上限 100 防止全表拉取 */
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private Integer pageSize = 20;

    /** 综合搜索关键词（用户名、昵称、真实姓名模糊匹配） */
    private String keyword;
    /** 用户名模糊查询 */
    private String username;
    /** 昵称模糊查询 */
    private String nickname;
    /** 用户类别筛选：backend / frontend */
    private String userCategory;
    /** 用户类型筛选：superadmin / tenant_admin / dept_admin / normal */
    private String userType;
    /** 用户状态筛选 */
    private String status;
    /** 部门 ID 筛选 */
    private String deptId;

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

    /** 返回安全的排序方向，默认 desc */
    public String safeOrderDirection() {
        if ("asc".equalsIgnoreCase(orderDirection)) {
            return "ASC";
        }
        return "DESC";
    }
}
