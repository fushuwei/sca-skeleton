package io.github.fushuwei.scaskeleton.gateway.handler;

import io.github.fushuwei.scaskeleton.core.constant.GlobalConstants;
import io.github.fushuwei.scaskeleton.core.exception.ErrorCode;
import io.github.fushuwei.scaskeleton.core.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.gateway.support.ServiceUnavailableException;
import org.springframework.cloud.gateway.support.TimeoutException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
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

    /** 404：无匹配路由或资源不存在 */
    private static final String MSG_NOT_FOUND = "接口不存在";

    /** 503：下游无可用实例或上游暂时不可用 */
    private static final String MSG_SERVICE_UNAVAILABLE = "服务暂时不可用，请稍后重试";

    /** 502：无法与上游建立连接 */
    private static final String MSG_BAD_GATEWAY = "网关连接上游服务失败";

    /** 504：等待上游响应超时 */
    private static final String MSG_GATEWAY_TIMEOUT = "网关等待上游响应超时";

    /** 500：未识别异常兜底文案（对外不暴露内部细节） */
    private static final String MSG_INTERNAL = "系统繁忙，请稍后重试";

    /**
     * JSON 序列化器
     */
    private final ObjectMapper objectMapper;

    /**
     * 网关异常入口
     */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // 判断响应是否已提交，如果已提交则无法改写，只能原样向上抛出
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        // 按异常类型解析 HTTP 状态、业务码与对外文案
        ResolvedGatewayError resolved = resolveError(ex);
        if (resolved.isUnauthorized()) {
            return handleUnauthorized(exchange, (UnauthorizedException) NestedExceptionUtils.getMostSpecificCause(ex));
        }

        // 记录网关侧错误日志后写出 JSON 响应体
        logGatewayError(exchange, ex, resolved.httpStatus());
        return writeErrorResponse(exchange, resolved.httpStatus(), resolved.code(), resolved.message(), false);
    }

    /**
     * 将异常映射为对外 HTTP 状态、业务码与提示文案。
     *
     * @param ex 原始异常（含包装链）
     * @return 解析结果；未识别时由调用方前的分支返回 500 兜底
     */
    private ResolvedGatewayError resolveError(Throwable ex) {
        Throwable root = NestedExceptionUtils.getMostSpecificCause(ex);

        // 网关安全门禁：无 Bearer / 令牌无效（由 Filter 主动抛出）
        if (root instanceof UnauthorizedException) {
            return ResolvedGatewayError.forUnauthorized();
        }
        // SCG：无匹配路由（404）或找不到服务实例（503）
        if (root instanceof NotFoundException nfe) {
            return fromResponseStatus(nfe.getStatusCode(), true);
        }
        // SCG：超过 gateway 配置的 response-timeout
        if (root instanceof TimeoutException) {
            return ResolvedGatewayError.of(HttpStatus.GATEWAY_TIMEOUT, ErrorCode.GATEWAY_TIMEOUT.getCode(),
                MSG_GATEWAY_TIMEOUT);
        }
        // SCG：上游服务暂时不可用
        if (root instanceof ServiceUnavailableException) {
            return ResolvedGatewayError.of(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.SERVICE_UNAVAILABLE.getCode(),
                MSG_SERVICE_UNAVAILABLE);
        }
        // 异常链中存在连接失败（含 Netty ConnectTimeoutException）
        if (isConnectFailure(ex)) {
            return ResolvedGatewayError.of(HttpStatus.BAD_GATEWAY, ErrorCode.BAD_GATEWAY.getCode(), MSG_BAD_GATEWAY);
        }
        // 读超时、写超时、连接池 acquire 超时等
        if (isGatewayTimeout(ex)) {
            return ResolvedGatewayError.of(HttpStatus.GATEWAY_TIMEOUT, ErrorCode.GATEWAY_TIMEOUT.getCode(),
                MSG_GATEWAY_TIMEOUT);
        }
        // 其它带 HTTP 状态的框架异常（按状态码映射）
        if (root instanceof ResponseStatusException rse) {
            return fromResponseStatus(rse.getStatusCode(), false);
        }

        // 兜底：对外统一 500，详细堆栈仅写日志
        return ResolvedGatewayError.of(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR.getCode(),
            MSG_INTERNAL);
    }

    /**
     * 按 {@link ResponseStatusException} 的 HTTP 状态映射；无法识别时归 500。
     *
     * @param statusCode      异常携带的 HTTP 状态
     * @param notFoundFamily  是否为 {@link NotFoundException} 一族（4xx 可保留原状态）
     * @return 解析结果
     */
    private ResolvedGatewayError fromResponseStatus(HttpStatusCode statusCode, boolean notFoundFamily) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        if (status == null) {
            return ResolvedGatewayError.of(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR.getCode(),
                MSG_INTERNAL);
        }
        return switch (status) {
            case NOT_FOUND -> ResolvedGatewayError.of(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND.getCode(), MSG_NOT_FOUND);
            case BAD_GATEWAY -> ResolvedGatewayError.of(HttpStatus.BAD_GATEWAY, ErrorCode.BAD_GATEWAY.getCode(),
                MSG_BAD_GATEWAY);
            case SERVICE_UNAVAILABLE -> ResolvedGatewayError.of(HttpStatus.SERVICE_UNAVAILABLE,
                ErrorCode.SERVICE_UNAVAILABLE.getCode(), MSG_SERVICE_UNAVAILABLE);
            case GATEWAY_TIMEOUT -> ResolvedGatewayError.of(HttpStatus.GATEWAY_TIMEOUT, ErrorCode.GATEWAY_TIMEOUT.getCode(),
                MSG_GATEWAY_TIMEOUT);
            case INTERNAL_SERVER_ERROR -> ResolvedGatewayError.of(HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR.getCode(), MSG_INTERNAL);
            // NotFoundException 的其它 4xx：保留原状态；其余未知状态归 500
            default -> notFoundFamily && status.is4xxClientError()
                ? ResolvedGatewayError.of(status, String.valueOf(status.value()), status.getReasonPhrase())
                : ResolvedGatewayError.of(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR.getCode(),
                    MSG_INTERNAL);
        };
    }

    /**
     * 连接被拒绝、无法建立 TCP 等上游不可达场景。
     *
     * @param ex 原始异常
     * @return 异常链中是否包含 {@link ConnectException}
     */
    private static boolean isConnectFailure(Throwable ex) {
        return hasCauseOfType(ex, ConnectException.class);
    }

    /**
     * 读超时、响应超时、连接池等待超时等。
     *
     * @param ex 原始异常
     * @return 是否为网关/网络超时类异常
     */
    private static boolean isGatewayTimeout(Throwable ex) {
        if (hasCauseOfType(ex, java.util.concurrent.TimeoutException.class)
            || hasCauseOfType(ex, SocketTimeoutException.class)) {
            return true;
        }
        // Netty / 连接池类名因依赖版本可能不同，按后缀匹配避免强耦合
        String name = ex.getClass().getName();
        return name.endsWith("ReadTimeoutException") || name.endsWith("WriteTimeoutException")
            || name.endsWith("PoolAcquireTimeoutException");
    }

    /**
     * 沿 cause 链判断是否存在指定类型的异常。
     *
     * @param ex   起始异常
     * @param type 目标类型
     * @return 是否匹配
     */
    private static boolean hasCauseOfType(Throwable ex, Class<? extends Throwable> type) {
        Throwable current = ex;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * 处理未认证：401 + {@code WWW-Authenticate: Bearer} + 业务 JSON。
     *
     * @param exchange     当前交换
     * @param unauthorized 未认证异常
     * @return 完成信号
     */
    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, UnauthorizedException unauthorized) {
        ServerHttpRequest request = exchange.getRequest();
        // 记录门禁拒绝日志（不打印 token，避免泄露）
        log.warn("[Gateway] unauthorized. traceId={} method={} path={} message={}",
            request.getHeaders().getFirst(GlobalConstants.HEADER_TRACE_ID),
            request.getMethod(), request.getPath().pathWithinApplication().value(), unauthorized.getMessage());
        return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, unauthorized.getCode(),
            unauthorized.getMessage(), true);
    }

    /**
     * 记录网关错误日志：5xx 打 error 全栈，其余打 warn。
     *
     * @param exchange   当前交换
     * @param ex         原始异常
     * @param httpStatus 对外 HTTP 状态
     */
    private void logGatewayError(ServerWebExchange exchange, Throwable ex, HttpStatus httpStatus) {
        ServerHttpRequest request = exchange.getRequest();
        String traceId = request.getHeaders().getFirst(GlobalConstants.HEADER_TRACE_ID);
        String logMsg = "[Gateway] error. traceId={} method={} path={} status={}";
        Object[] args = {traceId, request.getMethod(), request.getPath().pathWithinApplication().value(),
            httpStatus.value()};
        if (httpStatus.is5xxServerError()) {
            log.error(logMsg, args, ex);
        } else {
            log.warn(logMsg, args, ex);
        }
    }

    /**
     * 写入与下游 {@code ApiResponse} 对齐的三段式 JSON 错误响应体。
     *
     * @param exchange              当前交换
     * @param httpStatus            HTTP 状态
     * @param code                  业务错误码
     * @param message               对外提示文案
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
        } catch (JacksonException jacksonException) {
            // 序列化失败时仅结束响应，避免二次异常
            log.warn("Failed to serialize gateway error response, traceId={}, status={}",
                exchange.getRequest().getHeaders().getFirst(GlobalConstants.HEADER_TRACE_ID), httpStatus.value());
            return response.setComplete();
        }
    }

    /**
     * 异常解析结果（HTTP 状态 + 业务码 + 对外文案）。
     */
    private static final class ResolvedGatewayError {

        private final HttpStatus httpStatus;
        private final String code;
        private final String message;
        /** 是否为 401 未认证（需走专用分支写 WWW-Authenticate） */
        private final boolean unauthorized;

        private ResolvedGatewayError(HttpStatus httpStatus, String code, String message, boolean unauthorized) {
            this.httpStatus = httpStatus;
            this.code = code;
            this.message = message;
            this.unauthorized = unauthorized;
        }

        /** 标记为未认证，具体 code/message 由 {@link UnauthorizedException} 提供 */
        static ResolvedGatewayError forUnauthorized() {
            return new ResolvedGatewayError(HttpStatus.UNAUTHORIZED, null, null, true);
        }

        static ResolvedGatewayError of(HttpStatus httpStatus, String code, String message) {
            return new ResolvedGatewayError(httpStatus, code, message, false);
        }

        HttpStatus httpStatus() {
            return httpStatus;
        }

        String code() {
            return code;
        }

        String message() {
            return message;
        }

        boolean isUnauthorized() {
            return unauthorized;
        }
    }
}
