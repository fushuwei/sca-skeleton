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
 * 用户上下文全局过滤器
 * <p>
 * 职责：将 Spring Security 已完成认证的用户信息，以请求头形式注入到下游请求中，
 * 供后续业务服务无感知地读取当前用户（避免每个下游服务再次调用 {@code /oauth2/introspect}）。
 * <p>
 * <strong>本过滤器不做认证</strong>。OAuth2 不透明令牌的校验由更早的 Spring Security
 * 资源服务器过滤链（{@code GatewaySecurityConfiguration}）完成，认证结果以
 * {@link BearerTokenAuthentication} 形式存放在响应式 {@link SecurityContext} 中；
 * 本过滤器只负责"读取已有的认证结果 → 写入下游请求头"，是纯粹的上下文传递。
 * <p>
 * 注入字段：
 * <ul>
 *   <li>{@code X-User-Id}      ← 自省响应 sub claim（用户业务 ID）</li>
 *   <li>{@code X-User-Name}    ← 自省响应 preferred_username claim（登录名）</li>
 *   <li>{@code X-Tenant-Id}    ← 自省响应 tenant_id claim（租户 ID）</li>
 *   <li>{@code X-Trace-Id}     ← 链路追踪 ID（请求带值则沿用，缺失时生成）</li>
 * </ul>
 *
 * @author Fu Wei
 */
@Slf4j
@Component
public class UserContextGlobalFilter implements GlobalFilter, Ordered {

    /**
     * 从安全上下文读取认证结果并注入下游用户头 / TraceId
     * <p>
     * 处理分两条路径：
     * <ol>
     *   <li>SecurityContext 有 {@link BearerTokenAuthentication}（受保护路径）
     *       → 写入全部用户头 + TraceId 后放行</li>
     *   <li>SecurityContext 为空（白名单路径未认证）
     *       → 仅写入 TraceId 后放行，不写用户头</li>
     * </ol>
     *
     * @param exchange 当前请求交换
     * @param chain    过滤器链
     * @return Mono 完成信号
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1) 先确定 TraceId：上游已带则复用（如来自服务间调用），否则生成 32 位 UUID
        String traceId = exchange.getRequest().getHeaders().getFirst(GlobalConstants.HEADER_TRACE_ID);
        if (!StringUtils.hasText(traceId)) {
            traceId = UuidUtils.nextSimpleStr();
        }
        // lambda 捕获要求 effectively final，单独提一个变量
        final String finalTraceId = traceId;

        // 2) 从响应式 SecurityContext 读取认证结果（由 Spring Security 在更早阶段填充）
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            // 仅接受 OAuth2 不透明令牌自省产物，过滤匿名 / 其他认证类型
            .filter(auth -> auth instanceof BearerTokenAuthentication)
            .cast(BearerTokenAuthentication.class)
            // 拿到自省响应中的全部 claims（即 RFC 7662 的字段集合，含 sub / preferred_username 等）
            .map(BearerTokenAuthentication::getTokenAttributes)
            // 3a) 有认证：写入用户头 + TraceId，然后放行
            .flatMap(attrs -> chain.filter(buildExchangeWithUserHeaders(exchange, attrs, finalTraceId)))
            // 3b) 无认证（白名单）：仅写 TraceId，跳过用户头，放行
            .switchIfEmpty(chain.filter(exchange.mutate()
                .request(r -> r.headers(headers -> headers.set(GlobalConstants.HEADER_TRACE_ID, finalTraceId)))
                .build()));
    }

    /**
     * 基于自省响应属性，构造一个携带用户头与 TraceId 的新 exchange
     * <p>
     * 不修改原 exchange，而是通过 {@code mutate()} 生成不可变副本，符合 WebFlux 响应式语义。
     * 字段名严格对齐 OAuth2 / OIDC 标准（{@code sub}、{@code preferred_username}）与
     * 03/04 规则中约定的内部头名（{@code X-User-Id}、{@code X-User-Name}、{@code X-Tenant-Id}）。
     *
     * @param exchange 原始请求交换
     * @param attrs    自省响应属性（{@link BearerTokenAuthentication#getTokenAttributes()}）
     * @param traceId  链路追踪 ID
     * @return 写入用户头 / TraceId 后的新 exchange
     */
    private ServerWebExchange buildExchangeWithUserHeaders(
        ServerWebExchange exchange, Map<String, Object> attrs, String traceId) {

        // 从自省 claims 中提取业务字段，缺失返回 null（由后续条件判断兜底）
        String userId = stringAttr(attrs, "sub");
        String username = stringAttr(attrs, "preferred_username");
        String tenantId = stringAttr(attrs, "tenant_id");

        // 通过 ServerHttpRequest#mutate 派生新的请求对象，原对象保持不可变
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
            .headers(headers -> {
                // 用户 ID 头：仅在 claim 存在时写入，避免落下空字符串污染下游判断
                if (StringUtils.hasText(userId)) {
                    headers.set(GlobalConstants.HEADER_USER_ID, userId);
                }
                // 登录名头：同上，仅在有值时写入
                if (StringUtils.hasText(username)) {
                    headers.set(GlobalConstants.HEADER_USER_NAME, username);
                }
                // 租户 ID 头：单租户场景可能为空字符串，此处统一按"有值才写"处理
                if (StringUtils.hasText(tenantId)) {
                    headers.set(GlobalConstants.HEADER_TENANT_ID, tenantId);
                }
                // TraceId 头：无条件写入，保证全链路日志一定能串联
                headers.set(GlobalConstants.HEADER_TRACE_ID, traceId);
            })
            .build();

        // 用新 request 派生新 exchange 返回，后续过滤器看到的就是这份带头的版本
        return exchange.mutate().request(mutatedRequest).build();
    }

    /**
     * 从自省属性 Map 中安全地取出字符串值
     * <p>
     * 自省响应的字段类型并不严格固定（JSON 反序列化可能得到 String/Number/Boolean），
     * 统一通过 {@link Object#toString()} 兜底，避免在调用方做类型分支。
     *
     * @param attrs 自省属性 Map，可能为 null
     * @param key   字段名
     * @return 字符串值；Map 为空或 key 不存在时返回 null
     */
    private static String stringAttr(Map<String, Object> attrs, String key) {
        if (attrs == null) {
            return null;
        }
        Object v = attrs.get(key);
        return v != null ? v.toString() : null;
    }

    /**
     * 过滤器执行顺序：order = {@code HIGHEST_PRECEDENCE + 20}
     * <p>
     * 选值依据：
     * <ul>
     *   <li>必须晚于 {@code RequestHeaderGovernanceGlobalFilter}（order=HIGHEST+10），
     *       因为后者负责清掉外部伪造的用户头，必须先清后写</li>
     *   <li>必须早于 {@code RequestRateLimiter}（order=0）与
     *       {@code NettyRoutingFilter}（order≈MAX-1），保证用户头在限流与转发前已就位</li>
     * </ul>
     *
     * @return 升序优先级数值
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
