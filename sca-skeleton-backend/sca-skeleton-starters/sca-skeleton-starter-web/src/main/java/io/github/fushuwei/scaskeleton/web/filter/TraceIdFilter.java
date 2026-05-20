package io.github.fushuwei.scaskeleton.web.filter;

import io.github.fushuwei.scaskeleton.core.constant.GlobalConstants;
import io.github.fushuwei.scaskeleton.core.trace.TraceContext;
import io.github.fushuwei.scaskeleton.core.uuid.UuidUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * TraceId 注入过滤器。
 * <p>
 * 每个 HTTP 请求进入时，从请求头 {@link GlobalConstants#HEADER_TRACE_ID} 中读取 TraceId；
 * 若请求头中不存在（直接请求、非网关转发），则自动生成一个新的 32 位 UUID 作为本次链路标识。
 * TraceId 写入 SLF4J MDC，使当前线程后续的所有日志自动附带链路信息，
 * 同时回写到响应头，方便调用方排查问题。
 * <p>
 * 请求处理结束后必须清理 MDC，防止线程池场景下链路 ID 污染下一次请求。
 *
 * @author Fu Wei
 */
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 优先读取请求头中由网关或上游服务透传的 TraceId
        String traceId = request.getHeader(GlobalConstants.HEADER_TRACE_ID);

        // 请求头不存在时，为当前请求生成新的链路 ID
        if (traceId == null || traceId.isBlank()) {
            traceId = UuidUtils.nextSimpleStr();
        }

        // 将 TraceId 写入 MDC，此后当前线程的所有日志将自动携带该值
        TraceContext.set(traceId);

        // 回写到响应头，便于客户端和 API 网关透传及排查
        response.setHeader(GlobalConstants.HEADER_TRACE_ID, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 清理 MDC，避免线程池复用时出现链路 ID 串扰
            TraceContext.clear();
        }
    }
}
