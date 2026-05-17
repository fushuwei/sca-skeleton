package io.github.fushuwei.scaskeleton.gateway.filter;

import io.github.fushuwei.scaskeleton.core.constant.GlobalConstants;
import io.github.fushuwei.scaskeleton.core.uuid.UuidUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * 请求头治理全局过滤器（统一处理请求头的传递与管控）。
 * <p>
 * <ul>
 *     <li>清理不可信或不应透传的请求头（防外部伪造内部调用与用户身份）</li>
 *     <li>由网关统一生成 {@code X-Trace-Id}，不信任外网/浏览器传入的 trace 值</li>
 *     <li>注入请求开始时间等链路元数据</li>
 * </ul>
 *
 * @author Fu Wei
 */
@Component
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
     * 治理入站请求头：剥离伪造头、强制写入网关侧 TraceId 与起始时间。
     *
     * @param request 原始请求
     * @return 治理后的请求
     */
    private ServerHttpRequest governRequestHeaders(ServerHttpRequest request) {
        return request.mutate().headers(headers -> {
            // 1) 防止外部请求伪造内部调用标记
            headers.remove(GlobalConstants.HEADER_FROM);

            // 2) 清理外部可伪造的用户身份头（下游从 SecurityContext 读取，不经网关注入）
            headers.remove(GlobalConstants.HEADER_TENANT_ID);
            headers.remove(GlobalConstants.HEADER_USER_ID);
            headers.remove(GlobalConstants.HEADER_USER_NAME);
            headers.remove(GlobalConstants.HEADER_USER_ROLES);

            // 3) 清理不应透传的 hop-by-hop 头，减少协议层问题
            HOP_BY_HOP_HEADERS.forEach(headers::remove);

            // 4) 注入统一起始时间，便于耗时统计
            headers.set(GlobalConstants.HEADER_START_TIME, String.valueOf(System.currentTimeMillis()));

            // 5) 不信任客户端 TraceId：先移除再写入网关生成的 32 位 UUID
            headers.remove(GlobalConstants.HEADER_TRACE_ID);
            headers.set(GlobalConstants.HEADER_TRACE_ID, UuidUtils.nextSimpleStr());
        }).build();
    }

    /**
     * 过滤器执行顺序（优先执行，确保后续过滤器拿到已标准化请求）。
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
