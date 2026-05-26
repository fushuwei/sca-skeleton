package io.github.fushuwei.scaskeleton.gateway.handler;

import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.core.result.ResultType;
import io.github.fushuwei.scaskeleton.gateway.constant.GatewayConstants;
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

        // 记录异常日志
        writeErrorLog(exchange, errorResult, ex);

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
                    return ErrorResult.of(HttpStatus.UNAUTHORIZED, ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage());
                }
                // 禁止访问（403）
                case HttpStatus.FORBIDDEN -> {
                    return ErrorResult.of(HttpStatus.FORBIDDEN, ResultCode.FORBIDDEN.getCode(), ResultCode.FORBIDDEN.getMessage());
                }
                // 请求的资源不存在（404）
                case HttpStatus.NOT_FOUND -> {
                    return ErrorResult.of(HttpStatus.NOT_FOUND, ResultCode.NOT_FOUND.getCode(), ResultCode.NOT_FOUND.getMessage());
                }
                // 网关异常（502）
                case HttpStatus.BAD_GATEWAY -> {
                    return ErrorResult.of(HttpStatus.BAD_GATEWAY, ResultCode.BAD_GATEWAY.getCode(), ResultCode.BAD_GATEWAY.getMessage());
                }
                // 服务不可用（503）
                case HttpStatus.SERVICE_UNAVAILABLE -> {
                    return ErrorResult.of(HttpStatus.SERVICE_UNAVAILABLE, ResultCode.SERVICE_UNAVAILABLE.getCode(), ResultCode.SERVICE_UNAVAILABLE.getMessage());
                }
                // 网关超时（504）
                case HttpStatus.GATEWAY_TIMEOUT -> {
                    return ErrorResult.of(HttpStatus.GATEWAY_TIMEOUT, ResultCode.GATEWAY_TIMEOUT.getCode(), ResultCode.GATEWAY_TIMEOUT.getMessage());
                }
                default -> {
                    // 服务器内部错误（500）
                    return ErrorResult.of(HttpStatus.INTERNAL_SERVER_ERROR, ResultCode.INTERNAL_SERVER_ERROR.getCode(), ResultCode.INTERNAL_SERVER_ERROR.getMessage());
                }
            }
        }

        // 默认兜底异常
        return ErrorResult.of(HttpStatus.INTERNAL_SERVER_ERROR, ResultCode.INTERNAL_SERVER_ERROR.getCode(), ResultCode.INTERNAL_SERVER_ERROR.getMessage());
    }

    /**
     * 记录异常日志
     */
    private void writeErrorLog(ServerWebExchange exchange, ErrorResult errorResult, Throwable ex) {
        String traceId = exchange.getAttribute(GatewayConstants.EXCHANGE_ATTRIBUTE_TRACE_ID);
        String logMessage = "[Gateway] {}: TraceId => {}, Code => {}, Message => {}, Detail => {}";
        // 5xx 记 error 便于告警，4xx 记 warn 避免噪声
        if (errorResult.httpStatus().is5xxServerError()) {
            log.error(logMessage, "Error", traceId, errorResult.code, errorResult.message, ex.getMessage());
        } else {
            log.warn(logMessage, "Warn", traceId, errorResult.code, errorResult.message, ex.getMessage());
        }
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

        // 设置 HTTP 响应内容类型
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        // 设置响应体
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", errorResult.code);
        body.put("message", errorResult.message);
        body.put("data", null);
        body.put("type", ResultType.FAILURE);
        body.put("traceId", exchange.getAttribute(GatewayConstants.EXCHANGE_ATTRIBUTE_TRACE_ID));
        body.put("timestamp", System.currentTimeMillis());

        try {
            // 序列化为 JSON 并写入响应体
            DataBuffer buffer = response.bufferFactory().wrap(objectMapper.writeValueAsBytes(body));
            return response.writeWith(Mono.just(buffer));
        } catch (Throwable e) {
            // 序列化失败时仅结束响应，避免二次异常
            log.error("网关全局异常处理出现 JSON 序列化异常", e);
            return response.setComplete();
        }
    }

    /**
     * 异常响应结果
     *
     * @param httpStatus HTTP 状态码
     * @param code       业务错误码
     * @param message    对外提示信息
     */
    private record ErrorResult(HttpStatus httpStatus, Integer code, String message) {

        static ErrorResult of(HttpStatus httpStatus, Integer code, String message) {
            return new ErrorResult(httpStatus, code, message);
        }
    }
}
