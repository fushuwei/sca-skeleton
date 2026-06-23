package io.github.fushuwei.scaskeleton.web.exception;

import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.exception.ForbiddenException;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.core.result.ResultType;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author Fu Wei
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("[业务异常] code={}, message={}", e.getCode(), e.getMessage(), e);
        return Result.of(e.getCode(), e.getMessage(), ResultType.FAILURE);
    }

    /**
     * 处理授权异常（已认证但无权访问）
     */
    @ExceptionHandler(ForbiddenException.class)
    public Result<Void> handleForbiddenException(ForbiddenException e) {
        log.warn("[授权异常] {}", e.getMessage());
        return Result.fail(ResultCode.FORBIDDEN);
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public Result<Void> handleValidationException(Exception e) {
        log.warn("[参数校验异常] {}", e.getMessage());
        return Result.fail(ResultCode.VALIDATION_ERROR, e.getMessage());
    }

    /**
     * 处理请求体解析异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("[请求体解析异常] {}", e.getMessage());
        return Result.fail(ResultCode.VALIDATION_ERROR, "请求体格式错误或字段类型不正确");
    }

    /**
     * 捕获所有未被具体异常处理器处理的运行时异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("[服务器内部错误]", e);
        return Result.fail(ResultCode.INTERNAL_SERVER_ERROR);
    }

    /**
     * 处理兜底异常，捕获所有未预见的 Throwable 类型异常
     *
     * @param e 任意 {@link Throwable} 类型的异常实例
     * @return 统一的错误响应结果 {@link Result}
     */
    @ExceptionHandler(Throwable.class)
    public Result<?> handleThrowable(Throwable e) {
        log.error("[服务器内部错误]", e);
        return Result.fail(ResultCode.INTERNAL_SERVER_ERROR);
    }
}
