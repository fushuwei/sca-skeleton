package io.github.fushuwei.sca.gateway.filter;

import io.github.fushuwei.sca.starter.core.constant.GlobalConstants;
import io.github.fushuwei.sca.starter.core.util.UuidUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 用户上下文全局过滤器。
 * <p>
 * 在 JWT 验证通过后，从安全上下文提取 JWT Claims 并将用户信息注入下游请求头，
 * 下游服务（system 等）直接读取请求头获取用户上下文，无需再次解析 JWT。
 * <p>
 * 注入的请求头：
 * <ul>
 *   <li>{@code X-User-Id}    — 用户 ID（JWT sub claim）</li>
 *   <li>{@code X-Username}   — 登录用户名（JWT preferred_username claim）</li>
 *   <li>{@code X-Tenant-Id}  — 租户 ID（JWT tenant_id claim）</li>
 *   <li>{@code X-Trace-Id}   — 链路追踪 ID（从请求头读取或生成）</li>
 * </ul>
 *
 * @author Fu Wei
 */
@Slf4j
@Component
public class UserContextGlobalFilter implements GlobalFilter, Ordered {

    /**
     * 优先级高于路由过滤器，确保用户信息在转发前已注入
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 处理 TraceId：优先使用请求携带的，否则生成新的
        String traceId = exchange.getRequest().getHeaders()
            .getFirst(GlobalConstants.HEADER_TRACE_ID);
        if (!StringUtils.hasText(traceId)) {
            traceId = UuidUtils.generate();
        }
        final String finalTraceId = traceId;

        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter(auth -> auth instanceof JwtAuthenticationToken)
            .cast(JwtAuthenticationToken.class)
            .map(JwtAuthenticationToken::getToken)
            .flatMap(jwt -> chain.filter(buildExchangeWithUserHeaders(exchange, jwt, finalTraceId)))
            .switchIfEmpty(
                // 无 JWT（白名单路径），仅注入 TraceId
                chain.filter(exchange.mutate()
                    .request(r -> r.headers(headers ->
                        headers.set(GlobalConstants.HEADER_TRACE_ID, finalTraceId)))
                    .build())
            );
    }

    /**
     * 将 JWT Claims 写入请求头后，构建新的 ServerWebExchange 向下游转发。
     */
    private ServerWebExchange buildExchangeWithUserHeaders(
        ServerWebExchange exchange, Jwt jwt, String traceId) {

        String userId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        String tenantId = jwt.getClaimAsString("tenant_id");

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
            .headers(headers -> {
                if (StringUtils.hasText(userId)) {
                    headers.set(GlobalConstants.HEADER_USER_ID, userId);
                }
                if (StringUtils.hasText(username)) {
                    headers.set(GlobalConstants.HEADER_USERNAME, username);
                }
                if (StringUtils.hasText(tenantId)) {
                    headers.set("X-Tenant-Id", tenantId);
                }
                headers.set(GlobalConstants.HEADER_TRACE_ID, traceId);
            })
            .build();

        return exchange.mutate().request(mutatedRequest).build();
    }
}
