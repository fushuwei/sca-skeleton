package io.github.fushuwei.scaskeleton.feign.decoder;

import feign.Response;
import feign.codec.ErrorDecoder;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Feign 统一错误解码器。
 * <p>
 * 拦截下游服务返回的非 2xx 响应，根据 HTTP 状态码映射到对应的业务错误码，
 * 并将其包装为 {@link BusinessException} 向调用方抛出，
 * 使上游调用方能统一处理下游服务的错误，而不是接收到不明确的 FeignException。
 *
 * @author Fu Wei
 */
@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    // 默认错误解码器，用于处理未覆盖的状态码（如重试逻辑）
    private final ErrorDecoder defaultDecoder = new Default();

    /**
     * 将 Feign 下游响应中的错误解码为异常。
     *
     * @param methodKey Feign 方法标识，格式为 "ClassName#methodName()"
     * @param response  下游服务响应
     * @return 解码后的异常（通常为 BusinessException）
     */
    @Override
    public Exception decode(String methodKey, Response response) {
        // 读取响应体内容用于日志记录和错误信息透传
        String responseBody = readResponseBody(response);
        int status = response.status();

        log.error("[Feign] downstream error. method={} status={} body={}", methodKey, status, responseBody);

        // 根据 HTTP 状态码映射到对应业务错误码
        return switch (status) {
            // 下游服务返回 400：请求参数异常，原样透传
            case 400 -> new BusinessException(ErrorCode.INVALID_ARGUMENT,
                    "下游服务请求参数异常: " + responseBody);
            // 下游服务返回 401：未认证，可能是 Token 失效
            case 401 -> new BusinessException(ErrorCode.UNAUTHORIZED,
                    "下游服务认证失败: " + responseBody);
            // 下游服务返回 403：无权限访问该资源
            case 403 -> new BusinessException(ErrorCode.FORBIDDEN,
                    "下游服务访问被拒绝: " + responseBody);
            // 下游服务返回 404：目标资源不存在
            case 404 -> new BusinessException(ErrorCode.NOT_FOUND,
                    "下游服务资源不存在: " + responseBody);
            // 下游服务返回 429：下游限流触发
            case 429 -> new BusinessException(ErrorCode.TOO_MANY_REQUESTS,
                    "下游服务触发限流: " + responseBody);
            // 下游服务返回 503：服务不可用（可能正在重启或超载）
            case 503 -> new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
                    "下游服务暂不可用: " + responseBody);
            // 下游服务返回 5xx 其他错误：统一映射为内部错误
            default -> status >= 500
                    ? new BusinessException(ErrorCode.INTERNAL_ERROR, "下游服务内部错误: " + responseBody)
                    // 其他状态码交由默认解码器处理（如 RetryableException）
                    : defaultDecoder.decode(methodKey, response);
        };
    }

    /**
     * 读取响应体为字符串，用于日志记录和错误信息透传。
     * 响应体为空或读取失败时返回空字符串。
     *
     * @param response Feign 响应对象
     * @return 响应体字符串
     */
    private String readResponseBody(Response response) {
        if (response.body() == null) {
            return "";
        }
        try (InputStream inputStream = response.body().asInputStream()) {
            // 限制最大读取长度为 1KB，避免超大响应体占用过多内存
            byte[] bytes = inputStream.readNBytes(1024);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("[Feign] failed to read response body", e);
            return "[read error]";
        }
    }
}
