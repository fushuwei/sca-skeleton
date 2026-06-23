package io.github.fushuwei.scaskeleton.core.exception;

import io.github.fushuwei.scaskeleton.core.result.ResultCode;
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
    private final Integer code;

    /**
     * 使用自定义描述构造业务异常
     *
     * @param message 错误描述
     */
    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.FAILURE.getCode();
    }

    /**
     * 使用自定义错误码和描述构造业务异常
     *
     * @param code    业务错误码
     * @param message 错误描述
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 使用标准错误码构造业务异常，描述信息取自枚举
     *
     * @param resultCode 标准错误码枚举
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /**
     * 使用标准错误码构造业务异常，覆盖默认描述信息
     *
     * @param resultCode 标准错误码枚举
     * @param message    自定义错误描述
     */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
