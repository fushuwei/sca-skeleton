package io.github.fushuwei.sca.starter.web.exception;

import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.exception.ErrorCode;
import io.github.fushuwei.sca.starter.web.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * @author Fu Wei
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 处理业务异常并返回业务错误码。
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        // 组织业务异常响应体。
        ApiResponse<Void> body = ApiResponse.failure(exception.getCode(), exception.getMessage());
        // 使用 200 状态返回业务语义错误。
        return ResponseEntity.ok(body);
    }

    // 处理请求体参数校验异常。
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidationException(Exception exception) {
        // 构造统一参数错误响应体。
        ApiResponse<Void> body = ApiResponse.failure(ErrorCode.INVALID_ARGUMENT.getCode(), exception.getMessage());
        // 返回 400 状态表示客户端参数错误。
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 处理兜底异常。
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknownException(Exception exception) {
        // 构造统一系统错误响应体。
        ApiResponse<Void> body = ApiResponse.failure(ErrorCode.INTERNAL_ERROR.getCode(), exception.getMessage());
        // 返回 500 状态表示服务端异常。
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
