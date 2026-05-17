package io.github.fushuwei.scaskeleton.gateway.filter;

import io.github.fushuwei.scaskeleton.core.constant.GlobalConstants;
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
 * 网关安全前置过滤器
 *
 * @author Fu Wei
 */
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class GatewaySecurityGlobalFilter implements GlobalFilter, Ordered {

    /**
     * Ant 风格路径匹配器，用于白名单判定
     */
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /**
     * 网关安全相关配置（白名单等）
     */
    private final GatewaySecurityProperties gatewaySecurityProperties;

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
        // 3) 缺失令牌：抛异常，由 GatewayWebExceptionHandler 统一写 401，不转发下游
        return Mono.error(new UnauthorizedException("登录已过期，请重新登录"));
    }

    /**
     * 判断当前请求路径是否命中 {@code gateway.security.white-list} 中任一 Ant 模式
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
     * 从请求头解析 Bearer token 值
     */
    private static String extractBearerToken(ServerWebExchange exchange) {
        String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null) {
            return null;
        }
        if (auth.regionMatches(true, 0, GlobalConstants.BEARER_PREFIX, 0, GlobalConstants.BEARER_PREFIX.length())) {
            String token = auth.substring(GlobalConstants.BEARER_PREFIX.length()).trim();
            return StringUtils.hasText(token) ? token : null;
        }
        return null;
    }

    /**
     * 在请求头清理之后执行
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
