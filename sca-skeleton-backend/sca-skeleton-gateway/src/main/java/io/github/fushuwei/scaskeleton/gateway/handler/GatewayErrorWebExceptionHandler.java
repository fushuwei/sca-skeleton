package io.github.fushuwei.scaskeleton.gateway.handler;

import io.github.fushuwei.scaskeleton.core.constant.GlobalConstants;
import io.github.fushuwei.scaskeleton.core.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 网关全局 Web 异常处理器
 *
 * @author Fu Wei
 */
@Slf4j
@Component
@Order(-1)  // 确保优先级高于 Spring 默认的异常处理器
@RequiredArgsConstructor
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    /**
     * JSON 序列化器
     */
    private final ObjectMapper objectMapper;

    /**
     * 网关异常入口
     */
    @Override
    public @NonNull Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable ex) {
        // 判断响应是否已提交，如果已提交则无法改写，只能原样向上抛出
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        // 封装异常响应结果
        ErrorResult errorResult = resolveError(ex);

        // 记录日志
        String traceId = exchange.getRequest().getHeaders().getFirst(GlobalConstants.HEADER_TRACE_ID);
        String logMessage = "[Gateway] Error: traceId={}, code={}, message={}, errorMessage={}";
        if (errorResult.httpStatus().is5xxServerError()) {
            log.error(logMessage, traceId, errorResult.code, errorResult.message, ex.getMessage());
        } else {
            log.warn(logMessage, traceId, errorResult.code, errorResult.message, ex.getMessage());
        }

        // 返回响应体
        return writeErrorResponse(exchange, errorResult);
    }

    /**
     * 将异常映射为对外 HTTP 状态、业务码与提示信息
     */
    private ErrorResult resolveError(Throwable ex) {
        // 获取异常的具体原因
        Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(ex);

        // 处理 Spring 框架抛出的标准 HTTP 状态异常
        if (rootCause instanceof ResponseStatusException cause) {
            switch (cause.getStatusCode()) {
                // 未认证异常（401）
                case HttpStatus.UNAUTHORIZED -> {
                    return ErrorResult.of(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getCode(), ErrorMessage.UNAUTHORIZED.getMessage());
                }
                // 禁止访问（403）
                case HttpStatus.FORBIDDEN -> {
                    return ErrorResult.of(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getCode(), ErrorMessage.UNAUTHORIZED.getMessage());
                }
                // 请求的资源不存在（404）
                case HttpStatus.NOT_FOUND -> {
                    return ErrorResult.of(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND.getCode(), ErrorMessage.NOT_FOUND.getMessage());
                }
                // 网关异常（502）
                case HttpStatus.BAD_GATEWAY -> {
                    return ErrorResult.of(HttpStatus.BAD_GATEWAY, ErrorCode.BAD_GATEWAY.getCode(), ErrorMessage.BAD_GATEWAY.getMessage());
                }
                // 服务不可用（503）
                case HttpStatus.SERVICE_UNAVAILABLE -> {
                    return ErrorResult.of(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.SERVICE_UNAVAILABLE.getCode(), ErrorMessage.SERVICE_UNAVAILABLE.getMessage());
                }
                // 网关超时（504）
                case HttpStatus.GATEWAY_TIMEOUT -> {
                    return ErrorResult.of(HttpStatus.GATEWAY_TIMEOUT, ErrorCode.GATEWAY_TIMEOUT.getCode(), ErrorMessage.GATEWAY_TIMEOUT.getMessage());
                }
                default -> {
                    // 服务器内部错误（500）
                    return ErrorResult.of(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR.getCode(), ErrorMessage.INTERNAL_ERROR.getMessage());
                }
            }
        }

        // 默认兜底异常
        return ErrorResult.of(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR.getCode(), ErrorMessage.INTERNAL_ERROR.getMessage());
    }

    /**
     * 返回响应体
     */
    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, ErrorResult errorResult) {
        ServerHttpResponse response = exchange.getResponse();

        // 判断响应是否已提交，并发场景下可能已被其他处理器提交，直接结束
        if (response.isCommitted()) {
            return Mono.empty();
        }

        // 设置 HTTP 响应状态码与响应内容类型
        response.setStatusCode(errorResult.httpStatus);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        // 设置响应体
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", errorResult.code);
        body.put("message", errorResult.message);
        body.put("data", null);

        DataBuffer buffer = response.bufferFactory().wrap(objectMapper.writeValueAsBytes(body));
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 异常响应结果
     */
    private record ErrorResult(HttpStatus httpStatus, String code, String message) {

        static ErrorResult of(HttpStatus httpStatus, String code, String message) {
            return new ErrorResult(httpStatus, code, message);
        }
    }

    /**
     * 网关错误消息枚举
     */
    @Getter
    private enum ErrorMessage {

        /**
         * 401：未认证
         */
        UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录"),

        /**
         * 403：禁止访问
         */
        FORBIDDEN(HttpStatus.FORBIDDEN, "权限不足，拒绝访问"),

        /**
         * 404：请求的资源不存在
         */
        NOT_FOUND(HttpStatus.NOT_FOUND, "请求的资源不存在"),

        /**
         * 500：服务器内部错误
         */
        INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "系统繁忙，请稍后重试"),

        /**
         * 502：网关异常
         */
        BAD_GATEWAY(HttpStatus.BAD_GATEWAY, "服务响应异常，请稍后重试"),

        /**
         * 503：服务不可用
         */
        SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "服务暂时不可用，请稍后重试"),

        /**
         * 504：网关超时
         */
        GATEWAY_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "服务响应超时，请稍后重试");

        private final HttpStatus httpStatus;
        private final String message;

        ErrorMessage(HttpStatus httpStatus, String message) {
            this.httpStatus = httpStatus;
            this.message = message;
        }
    }
}
