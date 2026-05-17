package io.github.fushuwei.scaskeleton.gateway.filter;

import io.github.fushuwei.scaskeleton.core.exception.UnauthorizedException;
import io.github.fushuwei.scaskeleton.gateway.config.GatewaySecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关 Bearer 存在性校验过滤器。
 * <p>
 * 职责边界（与下游资源服务器分工）：
 * <ul>
 *   <li><strong>本过滤器</strong>：白名单放行；非白名单仅检查 {@code Authorization: Bearer &lt;token&gt;} 是否存在且非空，
 *       不访问 Redis、不调用自省端点、不解析令牌有效性</li>
 *   <li><strong>下游微服务</strong>：通过 {@code sca-skeleton-starter-security} 连接 Redis 完成真实鉴权与
 *       {@code @PreAuthorize} / {@code @RequiresPermission} 权限校验</li>
 * </ul>
 * <p>
 * 缺失 Bearer 时抛出 {@link UnauthorizedException}，由 {@link io.github.fushuwei.scaskeleton.gateway.handler.GatewayWebExceptionHandler} 统一写 401 JSON。
 *
 * @author Fu Wei
 */
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class GatewayBearerPresenceFilter implements GlobalFilter, Ordered {

    /**
     * Ant 风格路径匹配器，用于白名单判定。
     */
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /**
     * 网关白名单等配置。
     */
    private final GatewaySecurityProperties gatewaySecurityProperties;

    /**
     * @param gatewaySecurityProperties 白名单等网关安全属性
     */
    public GatewayBearerPresenceFilter(GatewaySecurityProperties gatewaySecurityProperties) {
        this.gatewaySecurityProperties = gatewaySecurityProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1) 白名单路径直接放行，无需 Bearer
        if (matchesWhiteList(exchange)) {
            return chain.filter(exchange);
        }
        // 2) 非白名单：要求 Authorization 头携带非空 Bearer token
        String bearerToken = extractBearerToken(exchange);
        if (StringUtils.hasText(bearerToken)) {
            return chain.filter(exchange);
        }
        // 3) 缺失令牌：抛异常，由 ErrorWebExceptionHandler 统一写 401，不转发下游
        return Mono.error(new UnauthorizedException("登录已过期，请重新登录"));
    }

    /**
     * 判断当前请求路径是否命中 {@code gateway.security.white-list} 中任一 Ant 模式。
     *
     * @param exchange 当前交换
     * @return true 表示在白名单内
     */
    private boolean matchesWhiteList(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        for (String pattern : gatewaySecurityProperties.getWhiteList()) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从请求头解析 Bearer token 值（不含 {@code Bearer } 前缀）。
     *
     * @param exchange 当前交换
     * @return token 字符串；缺失或格式不对时返回 null
     */
    private static String extractBearerToken(ServerWebExchange exchange) {
        String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null) {
            return null;
        }
        String prefix = "Bearer ";
        if (auth.regionMatches(true, 0, prefix, 0, prefix.length())) {
            String token = auth.substring(prefix.length()).trim();
            return StringUtils.hasText(token) ? token : null;
        }
        return null;
    }

    /**
     * 在请求头治理之后、限流与路由之前执行。
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 15;
    }
}
