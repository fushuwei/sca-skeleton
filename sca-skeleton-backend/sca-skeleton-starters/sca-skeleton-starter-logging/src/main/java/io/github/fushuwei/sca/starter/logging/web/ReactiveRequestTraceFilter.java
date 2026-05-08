package io.github.fushuwei.sca.starter.logging.web;

import io.github.fushuwei.sca.starter.core.id.UuidUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Reactive 请求链路追踪过滤器。
 *
 * @author Fu Wei
 */
public class ReactiveRequestTraceFilter implements GlobalFilter, Ordered {

    // HTTP Header 中 traceId 的键名。
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    // 为响应式请求补齐 traceId。
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取上游透传的 traceId。
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        // 当没有 traceId 时生成统一 ID。
        if (traceId == null || traceId.isBlank()) {
            // 生成新的 traceId 值。
            traceId = UuidUtils.nextSimpleStr();
        }
        // 将 traceId 注入请求头传递到下游服务。
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(builder -> builder.header(TRACE_ID_HEADER, traceId))
                .build();
        // 将 traceId 注入响应头方便链路跟踪。
        mutatedExchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);
        // 继续执行后续过滤链。
        return chain.filter(mutatedExchange);
    }

    // 设置较高优先级保证尽早写入 traceId。
    @Override
    public int getOrder() {
        // 返回靠前顺序值。
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
