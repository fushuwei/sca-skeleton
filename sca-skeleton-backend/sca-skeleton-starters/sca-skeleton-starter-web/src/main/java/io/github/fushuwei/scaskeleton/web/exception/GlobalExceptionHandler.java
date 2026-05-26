package io.github.fushuwei.scaskeleton.web.exception;

import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
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
        // 直接透传业务异常中的错误码与提示信息
        return Result.of(e.getCode(), e.getMessage(), ResultType.FAILURE);
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public Result<Void> handleValidationException(Exception e) {
        // 统一返回参数校验失败码，附带框架生成的校验提示
        return Result.fail(ResultCode.VALIDATION_ERROR, e.getMessage());
    }

    /**
     * 处理请求体反序列化异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        // JSON 反序列化失败时使用固定提示，避免暴露底层解析细节
        return Result.fail(ResultCode.VALIDATION_ERROR, "请求体格式错误或字段类型不正确");
    }

    /**
     * 捕获所有未被具体异常处理器处理的运行时异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        // 记录完整堆栈便于排查，对外仅返回通用 500 提示
        log.error("服务器内部错误", e);
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
        // 兜底捕获 Error 等非 Exception 类型，避免进程级异常直接暴露给客户端
        log.error("服务器内部错误", e);
        return Result.fail(ResultCode.INTERNAL_SERVER_ERROR);
    }
}
