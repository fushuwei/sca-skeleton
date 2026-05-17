package io.github.fushuwei.scaskeleton.core.exception;

/**
 * 未认证异常，身份缺失或访问凭证无效
 *
 * @author Fu Wei
 */
public class UnauthorizedException extends BusinessException {

    /**
     * 使用自定义描述构造 401 异常
     *
     * @param message 异常描述
     */
    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
