package io.github.fushuwei.scaskeleton.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.support.ipresolver.RemoteAddressResolver;
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * 网关限流配置
 *
 * @author Fu Wei
 */
@Configuration(proxyBeanMethods = false)
public class RateLimiterConfiguration {

    /**
     * 兜底限流 key，避免 KeyResolver 返回空导致 RequestRateLimiter 链路异常
     */
    private static final String ANONYMOUS_KEY = "anonymous";

    /**
     * 可信代理层数
     */
    private static final int TRUSTED_PROXY_COUNT = 1;

    /**
     * 客户端 IP 解析器，网关服务不能直接对外暴露，所有流量必须经过 Nginx 入口
     *
     * @return Spring Cloud Gateway 内置的 X-Forwarded-For 解析器
     */
    @Bean
    public RemoteAddressResolver remoteAddressResolver() {
        return XForwardedRemoteAddressResolver.maxTrustedIndex(TRUSTED_PROXY_COUNT);
    }

    /**
     * 按用户身份维度的限流键解析器
     * <p>
     * 解析顺序：access_token 的 {@code sub} → 客户端 IP → {@code "anonymous"}。
     * <p>
     * 适用前提：路由位于网关 Security 白名单之外（即必须先通过 OAuth2 自省校验），
     * 因此到达 RequestRateLimiter 时 SecurityContext 中应已存在
     * {@link BearerTokenAuthentication}；白名单接口理论上不会经过限流过滤器，
     * IP 兜底仅用于异常场景（如配置变更后白名单路由临时启用了限流）。
     *
     * @param remoteAddressResolver 客户端 IP 解析器
     * @return Reactor 风格的 {@link KeyResolver} 实现
     */
    @Bean("userKeyResolver")
    public KeyResolver userKeyResolver(RemoteAddressResolver remoteAddressResolver) {
        return exchange -> ReactiveSecurityContextHolder.getContext()
            // 1) 从安全上下文取认证对象，仅接受 OAuth2 不透明令牌自省产物
            .map(SecurityContext::getAuthentication)
            .filter(BearerTokenAuthentication.class::isInstance)
            .cast(BearerTokenAuthentication.class)
            // 2) 取自省响应中的 sub claim（与 03 规则定义的用户业务 ID 一致）
            .map(auth -> auth.getTokenAttributes().get("sub"))
            .filter(sub -> sub != null && !sub.toString().isBlank())
            .map(Object::toString)
            // 3) 未认证或 sub 缺失，回退到客户端 IP / 兜底常量
            .switchIfEmpty(Mono.fromSupplier(() -> resolveClientIp(exchange, remoteAddressResolver)));
    }

    /**
     * 通过注入的 {@link RemoteAddressResolver} 解析客户端 IP，地址为空时返回 {@link #ANONYMOUS_KEY}
     *
     * @param exchange 当前请求上下文
     * @param resolver 远程地址解析器
     * @return 客户端 IP 字符串或匿名兜底 key
     */
    private static String resolveClientIp(ServerWebExchange exchange, RemoteAddressResolver resolver) {
        InetSocketAddress addr = resolver.resolve(exchange);
        if (addr == null || addr.getAddress() == null) {
            return ANONYMOUS_KEY;
        }
        return addr.getAddress().getHostAddress();
    }
}
