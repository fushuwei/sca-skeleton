package io.github.fushuwei.scaskeleton.core.exception;

import lombok.Getter;

/**
 * 业务异常类，所有可预期的业务层错误均通过此类抛出
 *
 * @author Fu Wei
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 业务错误码
     */
    private final String code;

    /**
     * 使用自定义错误码和描述构造业务异常
     *
     * @param code    业务错误码
     * @param message 错误描述
     */
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 使用标准错误码构造业务异常，描述信息取自枚举
     *
     * @param errorCode 标准错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 使用标准错误码构造业务异常，覆盖默认描述信息
     *
     * @param errorCode 标准错误码枚举
     * @param message   自定义错误描述
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    /**
     * 使用标准错误码快速构造业务异常，描述信息取自枚举
     *
     * @param errorCode 标准错误码
     * @return 业务异常实例
     */
    public static BusinessException of(ErrorCode errorCode) {
        return new BusinessException(errorCode);
    }

    /**
     * 使用标准错误码快速构造业务异常，覆盖默认描述信息
     *
     * @param errorCode 标准错误码
     * @param message   自定义错误描述
     * @return 业务异常实例
     */
    public static BusinessException of(ErrorCode errorCode, String message) {
        return new BusinessException(errorCode, message);
    }
}
