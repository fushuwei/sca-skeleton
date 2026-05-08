package io.github.fushuwei.sca.starter.logging.web;

import io.github.fushuwei.sca.starter.core.id.UuidUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet 请求链路追踪过滤器。
 *
 * @author Fu Wei
 */
public class ServletRequestTraceFilter extends OncePerRequestFilter {

    // MDC 中 traceId 的键名。
    private static final String TRACE_ID_KEY = "traceId";
    // HTTP Header 中 traceId 的键名。
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    // 处理每次请求并注入链路标识。
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 优先读取上游透传的 traceId。
        String traceId = request.getHeader(TRACE_ID_HEADER);
        // 当上游未透传时生成新 traceId。
        if (traceId == null || traceId.isBlank()) {
            // 生成平台统一 UUID traceId。
            traceId = UuidUtils.nextSimpleStr();
        }
        // 将 traceId 放入 MDC 供日志系统输出。
        MDC.put(TRACE_ID_KEY, traceId);
        // 将 traceId 回写到响应头便于调用方关联日志。
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            // 继续执行后续过滤器链。
            filterChain.doFilter(request, response);
        } finally {
            // 请求完成后清理 MDC 防止线程复用污染。
            MDC.remove(TRACE_ID_KEY);
        }
    }
}
