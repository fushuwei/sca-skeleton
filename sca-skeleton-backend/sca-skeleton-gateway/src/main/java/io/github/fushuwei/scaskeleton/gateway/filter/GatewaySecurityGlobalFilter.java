package io.github.fushuwei.scaskeleton.gateway.filter;

import io.github.fushuwei.scaskeleton.core.constant.GlobalConstants;
import io.github.fushuwei.scaskeleton.core.util.BearerTokenUtils;
import io.github.fushuwei.scaskeleton.gateway.config.GatewaySecurityProperties;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关安全前置全局过滤器
 *
 * @author Fu Wei
 */
@Component
@RequiredArgsConstructor
public class GatewaySecurityGlobalFilter implements GlobalFilter, Ordered {

    /**
     * 网关安全相关配置（白名单等）
     */
    private final GatewaySecurityProperties gatewaySecurityProperties;

    /**
     * Ant 风格路径匹配器，用于白名单判定
     */
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        // 白名单路径直接放行，无需 Bearer
        if (matchesWhiteList(exchange)) {
            return chain.filter(exchange);
        }

        // 非白名单：要求 Authorization 头携带非空 Bearer token
        String bearerToken = BearerTokenUtils.extractBearerToken(
            exchange.getRequest().getHeaders().getFirst(GlobalConstants.HEADER_AUTHORIZATION));
        if (StringUtils.hasText(bearerToken)) {
            return chain.filter(exchange);
        }

        // 非白名单且缺失令牌：抛出 401 异常，不转发下游
        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    /**
     * 判断当前请求路径是否白名单
     */
    private boolean matchesWhiteList(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        for (String pattern : gatewaySecurityProperties.getPermitPaths()) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 在请求头清理之后执行
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
