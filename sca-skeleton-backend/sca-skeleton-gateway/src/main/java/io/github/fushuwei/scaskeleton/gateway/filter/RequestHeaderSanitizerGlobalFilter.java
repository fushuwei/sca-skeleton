package io.github.fushuwei.scaskeleton.gateway.filter;

import io.github.fushuwei.scaskeleton.core.constant.GlobalConstants;
import io.github.fushuwei.scaskeleton.core.uuid.UuidUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 请求头清理全局过滤器
 *
 * @author Fu Wei
 */
@Component
public class RequestHeaderSanitizerGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public @NonNull Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = sanitizeRequestHeaders(exchange.getRequest());
        return chain.filter(exchange.mutate().request(request).build());
    }

    /**
     * 清理请求头
     *
     * @param request 原始请求
     * @return 清理后的请求
     */
    private ServerHttpRequest sanitizeRequestHeaders(ServerHttpRequest request) {
        return request.mutate().headers(headers -> {
            // 1) 防止外部请求伪造内部调用标记
            headers.remove(GlobalConstants.HEADER_FROM);

            // 2) 清理外部可伪造的用户身份头
            headers.remove(GlobalConstants.HEADER_TENANT_ID);
            headers.remove(GlobalConstants.HEADER_USER_ID);
            headers.remove(GlobalConstants.HEADER_USER_NAME);
            headers.remove(GlobalConstants.HEADER_USER_DEPT);
            headers.remove(GlobalConstants.HEADER_USER_ROLES);

            // 3) 写入 TraceId
            headers.set(GlobalConstants.HEADER_TRACE_ID, UuidUtils.nextSimpleStr());

            // 4) 写入请求起始时间（用于耗时统计）
            headers.set(GlobalConstants.HEADER_REQUEST_START, String.valueOf(System.currentTimeMillis()));
        }).build();
    }

    /**
     * 过滤器执行顺序（优先执行，确保后续过滤器拿到已标准化请求）
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
