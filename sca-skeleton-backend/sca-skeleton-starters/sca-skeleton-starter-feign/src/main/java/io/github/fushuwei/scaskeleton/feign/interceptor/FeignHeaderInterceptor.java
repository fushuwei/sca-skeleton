package io.github.fushuwei.scaskeleton.feign.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.github.fushuwei.scaskeleton.core.constant.GlobalConstants;
import io.github.fushuwei.scaskeleton.core.trace.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 请求头传递拦截器。
 * <p>
 * 在 Feign 发出跨服务请求前，将以下请求头透传到下游服务：
 * <ul>
 *   <li>{@code X-Trace-Id}：链路追踪 ID，从当前线程 MDC 读取，确保跨服务日志可关联</li>
 *   <li>{@code Authorization}：JWT Bearer Token，从当前 HTTP 请求头中读取并透传</li>
 *   <li>{@code X-User-Id}：当前用户 ID，从当前 HTTP 请求头中透传（由网关注入）</li>
 *   <li>{@code X-Username}：当前用户名，从当前 HTTP 请求头中透传（由网关注入）</li>
 * </ul>
 *
 * @author Fu Wei
 */
@Slf4j
public class FeignHeaderInterceptor implements RequestInterceptor {

    /**
     * 在每次 Feign 请求发出前调用，将当前请求上下文的关键信息写入目标请求头。
     *
     * @param template Feign 请求模板，通过 header() 方法添加请求头
     */
    @Override
    public void apply(RequestTemplate template) {
        // 从当前线程 MDC 中读取链路追踪 ID 并透传
        String traceId = TraceContext.get();
        if (traceId != null && !traceId.isBlank()) {
            template.header(GlobalConstants.HEADER_TRACE_ID, traceId);
        }

        // 获取当前 HTTP 请求上下文（仅在 Web 请求线程中有效，异步线程需手动传递）
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            // 非 Web 上下文（定时任务、异步线程）无法获取 HTTP 请求头，跳过透传
            return;
        }

        HttpServletRequest request = attributes.getRequest();

        // 透传 Authorization Token，确保下游服务能完成鉴权
        String authorization = request.getHeader(GlobalConstants.HEADER_AUTHORIZATION);
        if (authorization != null && !authorization.isBlank()) {
            template.header(GlobalConstants.HEADER_AUTHORIZATION, authorization);
        }

        // 透传网关注入的用户 ID 和用户名，下游服务可直接读取而无需再次解析 Token
        String userId = request.getHeader(GlobalConstants.HEADER_USER_ID);
        if (userId != null && !userId.isBlank()) {
            template.header(GlobalConstants.HEADER_USER_ID, userId);
        }

        String username = request.getHeader(GlobalConstants.HEADER_USERNAME);
        if (username != null && !username.isBlank()) {
            template.header(GlobalConstants.HEADER_USERNAME, username);
        }
    }
}
