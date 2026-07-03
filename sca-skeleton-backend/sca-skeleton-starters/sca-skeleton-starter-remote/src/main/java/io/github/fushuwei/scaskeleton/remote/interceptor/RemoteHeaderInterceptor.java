package io.github.fushuwei.scaskeleton.remote.interceptor;

import io.github.fushuwei.scaskeleton.core.constant.GlobalConstants;
import io.github.fushuwei.scaskeleton.core.trace.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

/**
 * 远程调用请求头传递拦截器。
 * <p>
 * 在 RestClient 发出跨服务请求前，将以下上下文信息透传到下游服务：
 * <ul>
 *   <li>{@code X-Trace-Id}：链路追踪 ID，从当前线程 MDC 读取，确保跨服务日志可关联</li>
 *   <li>{@code Authorization}：Bearer Token，从当前 HTTP 请求头中读取并透传，确保下游服务能完成鉴权</li>
 * </ul>
 * <p>
 * 用户身份（userId、username 等）由下游资源服务器从 Bearer Token 自省获取，禁止透传已清理的内部头。
 * 非 Web 上下文（定时任务、异步线程）无法获取 HTTP 请求头，仅透传 TraceId。
 *
 * @author Fu Wei
 */
@Slf4j
public class RemoteHeaderInterceptor implements ClientHttpRequestInterceptor {

    /**
     * 在每次远程请求发出前调用，将当前请求上下文的关键信息写入目标请求头。
     *
     * @param request   HTTP 请求对象
     * @param body      请求体字节数组
     * @param execution 请求执行器
     * @return 下游服务响应
     * @throws IOException 请求执行过程中的 IO 异常
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        // 从当前线程 MDC 中读取链路追踪 ID 并透传
        String traceId = TraceContext.get();
        if (traceId != null && !traceId.isBlank()) {
            request.getHeaders().set(GlobalConstants.HEADER_TRACE_ID, traceId);
        }

        // 获取当前 HTTP 请求上下文（仅在 Web 请求线程中有效，异步线程需手动传递）
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            // 非 Web 上下文（定时任务、异步线程）无法获取 HTTP 请求头，跳过透传
            return execution.execute(request, body);
        }

        HttpServletRequest servletRequest = attributes.getRequest();

        // 透传 Authorization Token，确保下游服务能完成鉴权
        String authorization = servletRequest.getHeader(GlobalConstants.HEADER_AUTHORIZATION);
        if (authorization != null && !authorization.isBlank()) {
            request.getHeaders().set(GlobalConstants.HEADER_AUTHORIZATION, authorization);
        }

        return execution.execute(request, body);
    }
}
