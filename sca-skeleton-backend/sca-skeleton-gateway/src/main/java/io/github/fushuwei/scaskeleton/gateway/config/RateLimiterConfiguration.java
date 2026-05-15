package io.github.fushuwei.scaskeleton.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * 网关限流相关 Bean 配置
 * <p>
 * 当前提供 {@code userKeyResolver}：受保护路由优先按 access_token 的 {@code sub} 维度限流，
 * 取不到时回退为客户端 IP，再取不到回退为固定 {@code "anonymous"}。
 * <p>
 * Bean 名必须与 nacos 路由配置中 {@code #{@userKeyResolver}} 引用严格一致，
 * 否则 {@code RequestRateLimiter} 过滤器在路由初始化期会因 Bean 解析失败而启动报错。
 *
 * @author Fu Wei
 */
@Configuration
public class RateLimiterConfiguration {

    /**
     * 兜底限流 key，避免 KeyResolver 返回空导致 RequestRateLimiter 链路异常
     */
    private static final String ANONYMOUS_KEY = "anonymous";

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
     * @return Reactor 风格的 {@link KeyResolver} 实现
     */
    @Bean("userKeyResolver")
    public KeyResolver userKeyResolver() {
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
            .switchIfEmpty(Mono.fromSupplier(() -> resolveClientIp(exchange)));
    }

    /**
     * 取请求的远端地址；地址为空时返回 {@link #ANONYMOUS_KEY}
     * <p>
     * 注意：此处不解析 {@code X-Forwarded-For}，原因是网关作为整个系统的南北向入口，
     * 客户端可直接构造该头伪造 IP；如未来部署在可信反向代理之后，应在
     * {@link RequestHeaderGovernanceGlobalFilter} 中显式保留并由此处优先读取。
     *
     * @param exchange 当前请求上下文
     * @return 客户端 IP 字符串或匿名兜底 key
     */
    private static String resolveClientIp(ServerWebExchange exchange) {
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        if (remote == null || remote.getAddress() == null) {
            return ANONYMOUS_KEY;
        }
        return remote.getAddress().getHostAddress();
    }
}
