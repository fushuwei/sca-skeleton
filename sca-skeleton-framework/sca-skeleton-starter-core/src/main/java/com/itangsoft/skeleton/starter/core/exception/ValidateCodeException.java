package com.itangsoft.skeleton.starter.core.exception;

/**
 * 验证代码异常
 */
public class ValidateCodeException extends RuntimeException {

    private static final long serialVersionUID = -7285211528095468156L;

    public ValidateCodeException() {
    }

    public ValidateCodeException(String msg) {
        super(msg);
    }
}
