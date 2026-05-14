package io.github.fushuwei.scaskeleton.gateway.filter;

import io.github.fushuwei.scaskeleton.core.constant.GlobalConstants;
import io.github.fushuwei.scaskeleton.core.uuid.UuidUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 用户上下文全局过滤器。
 * <p>
 * 在 access_token 校验通过后，从 {@link BearerTokenAuthentication} 的 token 属性注入下游请求头。
 *
 * @author Fu Wei
 */
@Slf4j
@Component
public class UserContextGlobalFilter implements GlobalFilter, Ordered {

    /**
     * 优先级高于路由过滤器，确保用户信息在转发前已注入。
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    /**
     * 从安全上下文读取自省属性并写入 TraceId / 用户头。
     *
     * @param exchange 当前交换
     * @param chain    过滤器链
     * @return Mono 完成信号
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders()
            .getFirst(GlobalConstants.HEADER_TRACE_ID);
        if (!StringUtils.hasText(traceId)) {
            traceId = UuidUtils.nextSimpleStr();
        }
        final String finalTraceId = traceId;

        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter(auth -> auth instanceof BearerTokenAuthentication)
            .cast(BearerTokenAuthentication.class)
            .map(BearerTokenAuthentication::getTokenAttributes)
            .flatMap(attrs -> chain.filter(buildExchangeWithUserHeaders(exchange, attrs, finalTraceId)))
            .switchIfEmpty(
                chain.filter(exchange.mutate()
                    .request(r -> r.headers(headers ->
                        headers.set(GlobalConstants.HEADER_TRACE_ID, finalTraceId)))
                    .build())
            );
    }

    /**
     * 将 sub / preferred_username / tenant_id 写入请求头。
     *
     * @param exchange 交换
     * @param attrs    自省 token 属性
     * @param traceId  链路 ID
     * @return 变更后的 exchange
     */
    private ServerWebExchange buildExchangeWithUserHeaders(
        ServerWebExchange exchange, Map<String, Object> attrs, String traceId) {

        String userId = stringAttr(attrs, "sub");
        String username = stringAttr(attrs, "preferred_username");
        String tenantId = stringAttr(attrs, "tenant_id");

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
            .headers(headers -> {
                if (StringUtils.hasText(userId)) {
                    headers.set(GlobalConstants.HEADER_USER_ID, userId);
                }
                if (StringUtils.hasText(username)) {
                    headers.set(GlobalConstants.HEADER_USER_NAME, username);
                }
                if (StringUtils.hasText(tenantId)) {
                    headers.set("X-Tenant-Id", tenantId);
                }
                headers.set(GlobalConstants.HEADER_TRACE_ID, traceId);
            })
            .build();

        return exchange.mutate().request(mutatedRequest).build();
    }

    /**
     * 从属性 Map 取字符串，缺失返回 null。
     *
     * @param attrs Map
     * @param key   键
     * @return 字符串或 null
     */
    private static String stringAttr(Map<String, Object> attrs, String key) {
        if (attrs == null) {
            return null;
        }
        Object v = attrs.get(key);
        return v != null ? v.toString() : null;
    }
}
