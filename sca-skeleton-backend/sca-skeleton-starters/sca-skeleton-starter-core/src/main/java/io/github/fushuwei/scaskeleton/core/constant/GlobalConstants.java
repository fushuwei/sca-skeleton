package io.github.fushuwei.scaskeleton.core.constant;

/**
 * 全局公共常量定义。
 * <p>
 * 仅存放跨模块公用的、与业务无关的基础常量。
 * 各业务域的私有常量应定义在对应模块内，不得集中堆砌到此类。
 *
 * @author Fu Wei
 */
public final class GlobalConstants {

    private GlobalConstants() {
    }

    // ======================================================================
    // HTTP 请求头
    // ======================================================================

    /**
     * Authorization 请求头 Key
     */
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /**
     * Bearer Token 前缀（含尾部空格）
     */
    public static final String BEARER_PREFIX = "Bearer ";

    /**
     * 内部调用来源的请求头 Key
     */
    public static final String HEADER_FROM = "From";

    /**
     * 链路追踪 ID 请求头 Key
     */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    /**
     * 请求开始时间的请求头 Key
     */
    public static final String HEADER_START_TIME = "X-Request-Start-Time";

    /**
     * 透传当前租户 ID 的内部请求头 Key（网关 → 下游服务）
     */
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";

    /**
     * 透传当前用户 ID 的内部请求头 Key（网关 → 下游服务）
     */
    public static final String HEADER_USER_ID = "X-User-Id";

    /**
     * 透传当前用户名的内部请求头 Key（网关 → 下游服务）
     */
    public static final String HEADER_USER_NAME = "X-User-Name";

    /**
     * 透传当前用户角色的内部请求头 Key（网关 → 下游服务）
     */
    public static final String HEADER_USER_ROLES = "X-User-Roles";


    // ======================================================================
    // 逻辑删除
    // ======================================================================

    /**
     * 逻辑删除：未删除标识值
     */
    public static final int NOT_DELETED = 0;

    /**
     * 逻辑删除：已删除标识值
     */
    public static final int DELETED = 1;


    // ======================================================================
    // 通用状态
    // ======================================================================

    /**
     * 通用启用状态
     */
    public static final int STATUS_ENABLED = 1;

    /**
     * 通用禁用状态
     */
    public static final int STATUS_DISABLED = 0;
}
