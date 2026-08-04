package io.github.fushuwei.scaskeleton.web.exception;

import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.exception.ForbiddenException;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.core.result.ResultType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * 处理请求体参数校验异常（@Valid/@Validated 绑定错误）
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleBindException(BindException e) {
        log.warn("[参数校验异常] {}", e.getMessage());
        String message = Stream.concat(
                e.getBindingResult().getFieldErrors().stream().map(FieldError::getDefaultMessage),
                e.getBindingResult().getGlobalErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage))
            .filter(java.util.Objects::nonNull)
            .distinct()
            .collect(Collectors.joining("; "));
        return Result.fail(ResultCode.VALIDATION_ERROR, Optional.of(message).filter(s -> !s.isBlank()).orElse("请求参数校验失败"));
    }

    /**
     * 处理方法参数校验异常（@Validated 方法级约束）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("[参数校验异常] {}", e.getMessage());
        String message = e.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .collect(Collectors.joining("; "));
        return Result.fail(ResultCode.VALIDATION_ERROR, Optional.of(message).filter(s -> !s.isBlank()).orElse("请求参数校验失败"));
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
     * 处理上传文件大小超限异常（multipart 请求超过 max-file-size / max-request-size）
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("[上传文件超限] {}", e.getMessage());
        return Result.fail(ResultCode.VALIDATION_ERROR, "上传文件大小超出限制");
    }

    /**
     * 处理 404 异常（请求路径无匹配的静态资源或处理器）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("[资源不存在] {} {}", e.getHttpMethod(), e.getResourcePath());
        return Result.fail(ResultCode.NOT_FOUND);
    }

    /**
     * 捕获所有未被具体异常处理器处理的运行时异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("[服务器内部错误]", e);
        return Result.fail(ResultCode.INTERNAL_SERVER_ERROR);
    }
}
