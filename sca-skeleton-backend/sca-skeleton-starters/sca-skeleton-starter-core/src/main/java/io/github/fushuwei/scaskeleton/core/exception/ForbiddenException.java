package io.github.fushuwei.scaskeleton.core.exception;

import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import lombok.Getter;

/**
 * 授权异常类，已认证但无权限访问受保护资源时抛出
 *
 * @author Fu Wei
 */
@Getter
public class ForbiddenException extends RuntimeException {

    private final Integer code = ResultCode.FORBIDDEN.getCode();

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
