package io.github.fushuwei.sca.starter.core.exception;

/**
 * 统一错误码定义。
 * <p>
 * 编码分段规则：
 * <ul>
 *   <li>{@code 0}     — 操作成功</li>
 *   <li>{@code 4xx}   — 客户端错误（参数、认证、权限、资源未找到）</li>
 *   <li>{@code 5xx}   — 服务端错误（系统异常、服务不可用）</li>
 * </ul>
 * 业务域自定义错误码统一从 {@code A0001} 起，以大写字母区分业务域前缀。
 *
 * @author Fu Wei
 */
public enum ErrorCode {

    // 成功
    SUCCESS("0", "success"),

    // 客户端错误
    INVALID_ARGUMENT("400", "Invalid argument"),
    UNAUTHORIZED("401", "Unauthorized"),
    FORBIDDEN("403", "Forbidden"),
    NOT_FOUND("404", "Not found"),
    METHOD_NOT_ALLOWED("405", "Method not allowed"),
    CONFLICT("409", "Conflict"),
    TOO_MANY_REQUESTS("429", "Too many requests"),

    // 服务端错误
    INTERNAL_ERROR("500", "Internal server error"),
    SERVICE_UNAVAILABLE("503", "Service unavailable");

    // 错误码字符串
    private final String code;

    // 英文描述，用于日志与接口默认提示
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
