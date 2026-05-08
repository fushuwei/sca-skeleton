package io.github.fushuwei.sca.starter.core.exception;

/**
 * 统一错误码定义。
 *
 * @author Fu Wei
 */
public enum ErrorCode {
    // 成功状态码。
    SUCCESS("0", "success"),
    // 入参非法错误。
    INVALID_ARGUMENT("A0400", "invalid argument"),
    // 鉴权失败错误。
    UNAUTHORIZED("A0401", "unauthorized"),
    // 权限不足错误。
    FORBIDDEN("A0403", "forbidden"),
    // 资源不存在错误。
    NOT_FOUND("A0404", "resource not found"),
    // 业务逻辑冲突错误。
    BUSINESS_CONFLICT("A0509", "business conflict"),
    // 系统内部异常错误。
    INTERNAL_ERROR("B0500", "internal server error");

    // 错误码文本值。
    private final String code;
    // 错误描述信息。
    private final String message;

    // 构造枚举实例并写入错误码信息。
    ErrorCode(String code, String message) {
        // 初始化错误码值。
        this.code = code;
        // 初始化错误描述。
        this.message = message;
    }

    // 获取错误码文本。
    public String getCode() {
        // 返回当前错误码值。
        return code;
    }

    // 获取错误码描述。
    public String getMessage() {
        // 返回当前错误描述。
        return message;
    }
}
