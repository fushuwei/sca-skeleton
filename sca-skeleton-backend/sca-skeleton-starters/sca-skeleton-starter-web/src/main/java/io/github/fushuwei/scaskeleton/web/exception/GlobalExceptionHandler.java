package io.github.fushuwei.scaskeleton.web.exception;

import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.exception.ErrorCode;
import io.github.fushuwei.scaskeleton.core.exception.UnauthorizedException;
import io.github.fushuwei.scaskeleton.web.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
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

    /**
     * 未认证：HTTP 401，与网关、Security 入口语义对齐；message 取自抛出方传入的文案。
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(UnauthorizedException exception) {
        ApiResponse<Void> body = ApiResponse.failure(exception.getCode(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
            .body(body);
    }

    /**
     * 其它业务异常：HTTP 200 + body 内业务码（项目既有约定）。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ApiResponse<Void> body = ApiResponse.failure(exception.getCode(), exception.getMessage());
        return ResponseEntity.ok(body);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidationException(Exception exception) {
        ApiResponse<Void> body = ApiResponse.failure(ErrorCode.INVALID_ARGUMENT.getCode(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknownException(Exception exception) {
        ApiResponse<Void> body = ApiResponse.failure(ErrorCode.INTERNAL_ERROR.getCode(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
