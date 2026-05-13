package io.github.fushuwei.scaskeleton.gateway.filter;

import io.github.fushuwei.scaskeleton.core.constant.GlobalConstants;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 网关全局请求头治理过滤器（为后续鉴权、日志、审计提供可信且规范化的请求上下文）
 *
 * <ul>
 *     <li>清理不可信或不应透传的请求头（防外部伪造内部调用）</li>
 *     <li>注入统一链路元数据（如请求开始时间、TraceId）</li>
 * </ul>
 *
 * @author Fu Wei
 */
public class RequestHeaderGovernanceGlobalFilter implements GlobalFilter, Ordered {

    /**
     * RFC 7230 中不应被代理转发的 hop-by-hop 头。
     */
    private static final List<String> HOP_BY_HOP_HEADERS = Arrays.asList(
        HttpHeaders.CONNECTION,
        "Keep-Alive",
        HttpHeaders.PROXY_AUTHENTICATE,
        HttpHeaders.PROXY_AUTHORIZATION,
        HttpHeaders.TE,
        HttpHeaders.TRAILER,
        HttpHeaders.TRANSFER_ENCODING,
        HttpHeaders.UPGRADE
    );

    @Override
    public @NonNull Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = governRequestHeaders(exchange.getRequest());
        return chain.filter(exchange.mutate().request(request).build());
    }

    /**
     * 治理请求头
     */
    private ServerHttpRequest governRequestHeaders(ServerHttpRequest request) {
        return request.mutate().headers(headers -> {
            // 1) 防止外部请求伪造内部调用标记
            headers.remove(GlobalConstants.HEADER_FROM);

            // 2) 清理外部可伪造的用户身份头，后续由认证通过后重建
            headers.remove(GlobalConstants.HEADER_TENANT_ID);
            headers.remove(GlobalConstants.HEADER_USER_ID);
            headers.remove(GlobalConstants.HEADER_USER_NAME);
            headers.remove(GlobalConstants.HEADER_USER_ROLES);

            // 3) 清理不应透传的 hop-by-hop 头，减少协议层问题
            HOP_BY_HOP_HEADERS.forEach(headers::remove);

            // 4) 注入统一起始时间，便于耗时统计
            headers.set(GlobalConstants.HEADER_START_TIME, String.valueOf(System.currentTimeMillis()));

            // 5) 设置 Trace ID，用于日志追踪（上游有值则沿用）
            if (!StringUtils.hasText(headers.getFirst(GlobalConstants.HEADER_TRACE_ID))) {
                headers.set(GlobalConstants.HEADER_TRACE_ID, UUID.randomUUID().toString().replace("-", ""));
            }
        }).build();
    }

    @Override
    public int getOrder() {
        // 放在前置过滤链较靠前的位置，保证后续过滤器拿到已标准化请求
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
