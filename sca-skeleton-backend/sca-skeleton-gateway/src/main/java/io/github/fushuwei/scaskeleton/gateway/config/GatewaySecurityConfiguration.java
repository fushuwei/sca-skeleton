package io.github.fushuwei.scaskeleton.gateway.config;

import io.github.fushuwei.scaskeleton.gateway.filter.RequestHeaderGovernanceGlobalFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * 网关 Spring Security（响应式）最小化配置。
 * <p>
 * 职责边界（2025 架构调整后）：
 * <ul>
 *   <li><strong>不做</strong> OAuth2 不透明令牌自省、不做 Redis 令牌校验、不做方法级权限（403）</li>
 *   <li><strong>不做</strong>向下游注入 {@code X-User-Id} 等用户上下文头（由下游从 SecurityContext 读取）</li>
 *   <li><strong>由 {@link io.github.fushuwei.scaskeleton.gateway.filter.GatewayBearerPresenceFilter} 承担</strong>：
 *       白名单 + 非白名单 Bearer 存在性检查（缺失则 401）</li>
 *   <li><strong>由 {@link RequestHeaderGovernanceGlobalFilter} 承担</strong>：
 *       清理伪造内部头、注入 {@code X-Trace-Id} 等链路元数据</li>
 * </ul>
 *
 * @author Fu Wei
 */
@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
@EnableConfigurationProperties(GatewaySecurityProperties.class)
@RequiredArgsConstructor
public class GatewaySecurityConfiguration {

    /**
     * 注册请求头治理过滤器：清理伪造内部头、注入 TraceId / 起始时间。
     *
     * @return 全局过滤器实例
     */
    @Bean
    public RequestHeaderGovernanceGlobalFilter requestHeaderGovernanceGlobalFilter() {
        return new RequestHeaderGovernanceGlobalFilter();
    }

    /**
     * 构建放行全部路径的 SecurityWebFilterChain。
     * <p>
     * 认证前置逻辑已迁移至 {@link io.github.fushuwei.scaskeleton.gateway.filter.GatewayBearerPresenceFilter}，
     * 避免与本链重复校验；本链仅保留 WebFlux Security 基础设施与 CSRF 禁用。
     *
     * @param http ServerHttpSecurity
     * @return 反应式安全过滤链
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll());
        return http.build();
    }
}
