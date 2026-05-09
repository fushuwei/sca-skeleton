package io.github.fushuwei.sca.auth.web;

import io.github.fushuwei.sca.starter.core.exception.ErrorCode;
import io.github.fushuwei.sca.starter.web.response.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 安全相关异常映射，优先于全局兜底的 HTTP 语义更贴近鉴权失败。
 *
 * @author Fu Wei
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthSecurityExceptionHandler {

    // 认证失败统一 401。
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException exception) {
        // 使用统一错误体，消息取自框架异常。
        String message = exception.getMessage() != null ? exception.getMessage() : ErrorCode.UNAUTHORIZED.getMessage();
        ApiResponse<Void> body = ApiResponse.failure(ErrorCode.UNAUTHORIZED.getCode(), message);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // 授权失败统一 403。
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException exception) {
        String message = exception.getMessage() != null ? exception.getMessage() : ErrorCode.FORBIDDEN.getMessage();
        ApiResponse<Void> body = ApiResponse.failure(ErrorCode.FORBIDDEN.getCode(), message);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }
}
