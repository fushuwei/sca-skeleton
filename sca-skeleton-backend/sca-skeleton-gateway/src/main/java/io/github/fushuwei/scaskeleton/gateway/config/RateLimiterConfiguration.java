package io.github.fushuwei.scaskeleton.gateway.config;

import io.github.fushuwei.scaskeleton.core.constant.GlobalConstants;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.support.ipresolver.RemoteAddressResolver;
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * 网关限流配置。
 * <p>
 * 限流维度：优先按 {@code Authorization} 中的 Bearer token 字符串划分桶（同一令牌共享配额），
 * 无令牌时回退到客户端 IP。不依赖 Spring Security 上下文，与网关“仅检查 Bearer 存在性”策略一致。
 *
 * @author Fu Wei
 */
@Configuration(proxyBeanMethods = false)
public class RateLimiterConfiguration {

    /**
     * 兜底限流 key，避免 KeyResolver 返回空导致 RequestRateLimiter 链路异常。
     */
    private static final String ANONYMOUS_KEY = "anonymous";

    /**
     * 可信代理层数。
     */
    private static final int TRUSTED_PROXY_COUNT = 1;

    /**
     * 客户端 IP 解析器，网关服务不能直接对外暴露，所有流量必须经过 Nginx 入口。
     *
     * @return Spring Cloud Gateway 内置的 X-Forwarded-For 解析器
     */
    @Bean
    public RemoteAddressResolver remoteAddressResolver() {
        return XForwardedRemoteAddressResolver.maxTrustedIndex(TRUSTED_PROXY_COUNT);
    }

    /**
     * 按 Bearer token 字符串维度的限流键解析器。
     *
     * @param remoteAddressResolver 客户端 IP 解析器（无 token 时回退）
     * @return KeyResolver
     */
    @Bean("userKeyResolver")
    public KeyResolver userKeyResolver(RemoteAddressResolver remoteAddressResolver) {
        return exchange -> {
            String token = extractBearerToken(exchange);
            if (StringUtils.hasText(token)) {
                return Mono.just(token);
            }
            return Mono.just(resolveClientIp(exchange, remoteAddressResolver));
        };
    }

    /**
     * 从 Authorization 头解析 Bearer token 明文，作为限流 key。
     *
     * @param exchange 当前请求
     * @return token 值；缺失时返回 null
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
     * 通过 {@link RemoteAddressResolver} 解析客户端 IP，地址为空时返回 {@link #ANONYMOUS_KEY}。
     *
     * @param exchange              当前请求上下文
     * @param remoteAddressResolver 远程地址解析器
     * @return 客户端 IP 或匿名兜底 key
     */
    private static String resolveClientIp(ServerWebExchange exchange, RemoteAddressResolver remoteAddressResolver) {
        InetSocketAddress addr = remoteAddressResolver.resolve(exchange);
        if (addr == null || addr.getAddress() == null) {
            return ANONYMOUS_KEY;
        }
        return addr.getAddress().getHostAddress();
    }
}
