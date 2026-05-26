package io.github.fushuwei.scaskeleton.gateway.filter;

import io.github.fushuwei.scaskeleton.core.constant.GlobalConstants;
import io.github.fushuwei.scaskeleton.core.uuid.UuidUtils;
import io.github.fushuwei.scaskeleton.gateway.constant.GatewayConstants;
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
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, GatewayFilterChain chain) {
        // 清理伪造头、写入 TraceId 与请求起始时间，再交给后续过滤器
        ServerHttpRequest request = sanitizeRequestHeaders(exchange);
        return chain.filter(exchange.mutate().request(request).build());
    }

    /**
     * 清理请求头
     */
    private ServerHttpRequest sanitizeRequestHeaders(ServerWebExchange exchange) {
        return exchange.getRequest().mutate().headers(headers -> {
            // 1) 防止外部请求伪造内部调用标记
            headers.remove(GlobalConstants.HEADER_FROM);

            // 2) 清理外部可伪造的用户身份头
            headers.remove(GlobalConstants.HEADER_TENANT_ID);
            headers.remove(GlobalConstants.HEADER_USER_ID);
            headers.remove(GlobalConstants.HEADER_USER_NAME);
            headers.remove(GlobalConstants.HEADER_USER_DEPT);
            headers.remove(GlobalConstants.HEADER_USER_ROLES);

            // 3) 写入 TraceId
            String traceId = UuidUtils.nextSimpleStr();
            headers.set(GlobalConstants.HEADER_TRACE_ID, traceId);
            exchange.getAttributes().put(GatewayConstants.EXCHANGE_ATTRIBUTE_TRACE_ID, traceId);

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
