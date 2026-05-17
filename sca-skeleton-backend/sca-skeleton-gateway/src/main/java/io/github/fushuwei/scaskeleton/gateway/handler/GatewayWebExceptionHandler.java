package io.github.fushuwei.scaskeleton.gateway.handler;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import io.github.fushuwei.scaskeleton.core.constant.GlobalConstants;
import io.github.fushuwei.scaskeleton.core.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 网关全局 Web 异常处理器（{@link WebExceptionHandler}）。
 * <p>
 * 在 Spring Cloud Gateway 中，过滤器链抛出的 {@link UnauthorizedException} 等异常会进入 WebFlux
 * {@link WebExceptionHandler} 责任链；本类以较高优先级处理未认证场景，其余异常交还后续处理器。
 *
 * @author Fu Wei
 */
@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class GatewayWebExceptionHandler implements WebExceptionHandler {

    /**
     * JSON 序列化器，写出与下游 {@code ApiResponse} 一致的三段式结构。
     */
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(ex);
        if (rootCause instanceof UnauthorizedException unauthorized) {
            return handleUnauthorized(exchange, unauthorized);
        }
        return Mono.error(ex);
    }

    /**
     * 处理未认证异常：HTTP 401 + {@code WWW-Authenticate: Bearer} + 业务 JSON。
     *
     * @param exchange      当前交换
     * @param unauthorized  未认证异常（message 由抛出方指定）
     * @return 完成信号
     */
    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, UnauthorizedException unauthorized) {
        ServerHttpRequest request = exchange.getRequest();
        log.warn("[Gateway] unauthorized. method={} path={} message={}",
            request.getMethod(), request.getPath().pathWithinApplication().value(), unauthorized.getMessage());
        return writeApiErrorResponse(exchange, HttpStatus.UNAUTHORIZED, unauthorized.getCode(),
            unauthorized.getMessage(), true);
    }

    /**
     * 写入统一 JSON 错误响应体。
     *
     * @param exchange                当前交换
     * @param httpStatus              HTTP 状态
     * @param code                    业务错误码
     * @param message                 业务错误描述（来自异常，非 ErrorCode 默认文案）
     * @param wwwAuthenticateBearer   是否添加 {@code WWW-Authenticate: Bearer}
     * @return 完成信号
     */
    private Mono<Void> writeApiErrorResponse(ServerWebExchange exchange, HttpStatus httpStatus, String code,
            String message, boolean wwwAuthenticateBearer) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.empty();
        }
        response.setStatusCode(httpStatus);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if (wwwAuthenticateBearer) {
            response.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", null);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JacksonException ex) {
            log.warn("Failed to serialize gateway error response, traceId={}, status={}",
                exchange.getRequest().getHeaders().getFirst(GlobalConstants.HEADER_TRACE_ID), httpStatus.value());
            return response.setComplete();
        }
    }
}
