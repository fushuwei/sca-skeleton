package io.github.fushuwei.scaskeleton.gateway.handler;

import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
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
import reactor.core.publisher.Mono;

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
     * 网关异常入口：仅接管 {@link UnauthorizedException}，其余异常交还默认 {@link ErrorWebExceptionHandler} 链。
     *
     * @param exchange 当前交换
     * @param ex       过滤器链或路由阶段抛出的异常
     * @return 完成信号
     */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // 响应已提交则无法改写，原样向上抛出
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        // 剥离包装异常，定位业务根因
        Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(ex);
        if (rootCause instanceof UnauthorizedException unauthorized) {
            return handleUnauthorized(exchange, unauthorized);
        }
        // 非本处理器职责：交还后续 ErrorWebExceptionHandler（含 Spring 默认实现）
        return Mono.error(ex);
    }

    /**
     * 处理未认证异常
     *
     * @param exchange     当前交换
     * @param unauthorized 未认证异常（message 由抛出方指定）
     * @return 完成信号
     */
    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, UnauthorizedException unauthorized) {
        ServerHttpRequest request = exchange.getRequest();
        // 记录门禁拒绝日志（不打印 token，避免泄露）
        log.warn("[Gateway] unauthorized. method={} path={} message={}",
            request.getMethod(), request.getPath().pathWithinApplication().value(), unauthorized.getMessage());
        // 401 + WWW-Authenticate + 与下游一致的三段式 JSON
        return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, unauthorized.getCode(),
            unauthorized.getMessage(), true);
    }

    /**
     * 写入统一 JSON 错误响应体
     *
     * @param exchange              当前交换
     * @param httpStatus            HTTP 状态
     * @param code                  业务错误码
     * @param message               业务错误描述（来自异常，非 ErrorCode 默认文案）
     * @param wwwAuthenticateBearer 是否添加 {@code WWW-Authenticate: Bearer}
     * @return 完成信号
     */
    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus httpStatus, String code,
                                             String message, boolean wwwAuthenticateBearer) {
        ServerHttpResponse response = exchange.getResponse();
        // 并发场景下可能已被其他处理器提交，直接结束
        if (response.isCommitted()) {
            return Mono.empty();
        }
        // 设置 HTTP 状态与 JSON 内容类型
        response.setStatusCode(httpStatus);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if (wwwAuthenticateBearer) {
            response.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }

        // 与下游 ApiResponse 对齐：code / message / data
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", null);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JacksonException ex) {
            // 序列化失败时仅结束响应，避免二次异常
            log.warn("Failed to serialize gateway error response, traceId={}, status={}",
                exchange.getRequest().getHeaders().getFirst(GlobalConstants.HEADER_TRACE_ID), httpStatus.value());
            return response.setComplete();
        }
    }
}
