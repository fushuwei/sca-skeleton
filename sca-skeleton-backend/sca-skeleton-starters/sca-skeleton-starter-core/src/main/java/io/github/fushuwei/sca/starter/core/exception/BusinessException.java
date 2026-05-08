package io.github.fushuwei.sca.starter.core.exception;

/**
 * 统一业务异常。
 *
 * @author Fu Wei
 */
public class BusinessException extends RuntimeException {

    // 业务错误码。
    private final String code;

    // 使用错误枚举构造业务异常。
    public BusinessException(ErrorCode errorCode) {
        // 调用父类构造器写入错误描述。
        super(errorCode.getMessage());
        // 保存错误码用于响应输出。
        this.code = errorCode.getCode();
    }

    // 使用错误枚举和自定义消息构造业务异常。
    public BusinessException(ErrorCode errorCode, String message) {
        // 调用父类构造器写入自定义消息。
        super(message);
        // 保存错误码用于响应输出。
        this.code = errorCode.getCode();
    }

    // 获取错误码文本。
    public String getCode() {
        // 返回异常内错误码。
        return code;
    }
}
