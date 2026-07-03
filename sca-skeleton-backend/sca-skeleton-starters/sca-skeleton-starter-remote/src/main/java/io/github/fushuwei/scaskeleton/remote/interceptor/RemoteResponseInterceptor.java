package io.github.fushuwei.scaskeleton.remote.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 远程调用统一响应解码拦截器。
 * <p>
 * 核心职责：
 * <ol>
 *   <li><b>业务结果解包</b>：下游服务返回统一响应体 {@code Result<T>}，本拦截器自动解析并提取
 *       {@code data} 字段，使 HTTP Interface 方法可直接声明返回 {@code T} 而非 {@code Result<T>}，
 *       调用方无需手动判断 {@code isSuccess()} 和提取数据。</li>
 *   <li><b>业务错误转换</b>：当 {@code Result.code != SUCCESS} 时，将业务错误码和消息封装为
 *       {@link BusinessException} 向调用方抛出，保持与本地异常处理一致。</li>
 *   <li><b>HTTP 错误处理</b>：当 HTTP 状态码为 4xx/5xx 时，尝试解析响应体为 {@code Result} 获取
 *       错误详情；解析失败则按状态码映射为对应的 {@link BusinessException}。</li>
 *   <li><b>非标准响应透传</b>：当响应体不符合 {@code Result} 结构时（如文件下载、第三方 API），
 *       原样透传，不影响正常反序列化。</li>
 * </ol>
 * <p>
 * 拦截器执行顺序：HeaderInterceptor → <b>ResponseInterceptor（本类）</b> → 实际请求执行 → 响应处理。
 *
 * @author Fu Wei
 */
@Slf4j
public class RemoteResponseInterceptor implements ClientHttpRequestInterceptor {

    private final ObjectMapper objectMapper;

    /**
     * 构造方法。
     *
     * @param objectMapper Jackson ObjectMapper，用于解析 {@code Result<T>} 响应体
     */
    public RemoteResponseInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 拦截远程调用响应，执行统一解码和错误处理。
     *
     * @param request   HTTP 请求对象
     * @param body      请求体字节数组
     * @param execution 请求执行器
     * @return 处理后的响应（可能已替换响应体）
     * @throws IOException 请求执行过程中的 IO 异常
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        ClientHttpResponse response = execution.execute(request, body);

        // 读取响应体并缓存（响应流只能读取一次）
        byte[] responseBody = StreamUtils.copyToByteArray(response.getBody());

        // HTTP 状态码为错误时，走错误处理逻辑
        if (response.getStatusCode().isError()) {
            handleErrorResponse(request, response.getStatusCode(), responseBody);
        }

        // 尝试将响应体解析为 Result<?> 并解包
        return unwrapResultResponse(response, responseBody);
    }

    /**
     * 将 {@code Result<T>} 响应体解包，提取 {@code data} 字段。
     * <p>
     * 如果响应体不是 {@code Result} 结构（解析失败），原样透传。
     *
     * @param response     原始响应
     * @param responseBody 响应体字节数组
     * @return 解包后的响应（data 字段作为新响应体）；非 Result 结构时返回原始响应
     */
    private ClientHttpResponse unwrapResultResponse(ClientHttpResponse response, byte[] responseBody) {
        try {
            Result<?> result = objectMapper.readValue(responseBody, Result.class);
            if (result == null) {
                return new BufferedResponse(response, responseBody);
            }

            // 业务错误：code != SUCCESS，抛出 BusinessException
            if (!result.isSuccess()) {
                log.error("[Remote] business error. code={} message={}",
                        result.code(), result.message());
                throw new BusinessException(result.code(), result.message());
            }

            // 业务成功：提取 data 字段作为新响应体
            byte[] dataBytes;
            if (result.data() == null) {
                dataBytes = new byte[0];
            } else {
                dataBytes = objectMapper.writeValueAsBytes(result.data());
            }
            return new BufferedResponse(response, dataBytes);
        } catch (BusinessException e) {
            throw e; // 业务异常直接向上抛出
        } catch (Exception e) {
            // 响应体不是 Result 结构（如文件下载、第三方 API），原样透传
            log.debug("[Remote] response is not a Result wrapper, passing through as-is");
            return new BufferedResponse(response, responseBody);
        }
    }

    /**
     * 处理 HTTP 错误状态码响应。
     * <p>
     * 尝试解析响应体为 {@code Result} 获取下游业务错误详情；解析失败则按 HTTP 状态码
     * 映射为对应的 {@link BusinessException}。
     *
     * @param request      HTTP 请求对象
     * @param statusCode   HTTP 状态码
     * @param responseBody 响应体字节数组
     */
    private void handleErrorResponse(HttpRequest request, HttpStatusCode statusCode, byte[] responseBody) {
        String bodyStr = new String(responseBody, StandardCharsets.UTF_8);

        // 尝试从响应体中解析 Result 获取下游业务错误详情
        try {
            Result<?> result = objectMapper.readValue(responseBody, Result.class);
            if (result != null && result.code() != null) {
                log.error("[Remote] downstream business error. method={} status={} code={} message={}",
                        request.getURI(), statusCode.value(), result.code(), result.message());
                throw new BusinessException(result.code(), result.message());
            }
        } catch (BusinessException e) {
            throw e; // 业务异常直接向上抛出
        } catch (Exception ignored) {
            // 响应体不是 Result 结构，按 HTTP 状态码映射
        }

        log.error("[Remote] downstream http error. method={} status={} body={}",
                request.getURI(), statusCode.value(), bodyStr);

        // 限制错误消息长度，避免超大响应体导致日志膨胀
        String truncatedBody = bodyStr.length() > 1024 ? bodyStr.substring(0, 1024) + "..." : bodyStr;

        throw switch (statusCode.value()) {
            case 400 -> new BusinessException(ResultCode.VALIDATION_ERROR,
                    "下游服务请求参数异常: " + truncatedBody);
            case 401 -> new BusinessException(ResultCode.UNAUTHORIZED,
                    "下游服务认证失败: " + truncatedBody);
            case 403 -> new BusinessException(ResultCode.FORBIDDEN,
                    "下游服务访问被拒绝: " + truncatedBody);
            case 404 -> new BusinessException(ResultCode.NOT_FOUND,
                    "下游服务资源不存在: " + truncatedBody);
            case 429 -> new BusinessException(ResultCode.TOO_MANY_REQUESTS,
                    "下游服务触发限流: " + truncatedBody);
            case 503 -> new BusinessException(ResultCode.SERVICE_UNAVAILABLE,
                    "下游服务暂不可用: " + truncatedBody);
            default -> statusCode.is5xxServerError()
                    ? new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "下游服务内部错误: " + truncatedBody)
                    : new BusinessException(ResultCode.FAILURE, "下游服务返回异常: " + truncatedBody);
        };
    }

    /**
     * 缓冲响应包装器。
     * <p>
     * {@link ClientHttpResponse} 的 body 是一次性流，读取后无法再次消费。
     * 本类将响应体缓存为字节数组，支持多次读取，并在解包后替换为新的响应体。
     */
    private static class BufferedResponse implements ClientHttpResponse {

        private final ClientHttpResponse delegate;
        private final byte[] body;

        BufferedResponse(ClientHttpResponse delegate, byte[] body) {
            this.delegate = delegate;
            this.body = body;
        }

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return delegate.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return delegate.getStatusText();
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public HttpHeaders getHeaders() {
            return delegate.getHeaders();
        }
    }
}
