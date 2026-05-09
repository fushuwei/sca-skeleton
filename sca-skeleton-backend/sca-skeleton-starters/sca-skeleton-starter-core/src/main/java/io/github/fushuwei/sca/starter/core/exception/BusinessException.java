package io.github.fushuwei.sca.starter.core.exception;

/**
 * 业务异常基类。
 * <p>
 * 所有可预期的业务层错误均通过此类抛出，携带结构化错误码，
 * 由 Web 层全局异常处理器统一转换为 {@code ApiResponse} 响应体返回给调用方。
 * <p>
 * 使用示例：
 * <pre>{@code
 * throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
 * throw BusinessException.of(ErrorCode.CONFLICT);
 * }</pre>
 *
 * @author Fu Wei
 */
public class BusinessException extends RuntimeException {

    // 业务错误码，对应 ErrorCode 或自定义域错误码字符串
    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    /**
     * 使用标准错误码快速构造业务异常（message 使用 ErrorCode 默认描述）。
     *
     * @param errorCode 标准错误码
     * @return 业务异常实例
     */
    public static BusinessException of(ErrorCode errorCode) {
        return new BusinessException(errorCode);
    }

    /**
     * 使用标准错误码 + 自定义 message 构造业务异常。
     *
     * @param errorCode 标准错误码
     * @param message   自定义错误描述
     * @return 业务异常实例
     */
    public static BusinessException of(ErrorCode errorCode, String message) {
        return new BusinessException(errorCode, message);
    }

    public String getCode() {
        return code;
    }
}
