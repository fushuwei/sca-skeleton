package io.github.fushuwei.scaskeleton.gateway.config;

import io.github.fushuwei.scaskeleton.gateway.filter.RequestHeaderGovernanceGlobalFilter;
import io.github.fushuwei.scaskeleton.gateway.handler.GatewayAccessDeniedHandler;
import io.github.fushuwei.scaskeleton.gateway.handler.GatewayAuthenticationEntryPoint;
import io.github.fushuwei.scaskeleton.gateway.security.PermissionsReactiveOpaqueTokenAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringReactiveOpaqueTokenIntrospector;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.util.StringUtils;

/**
 * 网关 Spring Security（响应式）安全配置
 * <p>
 * 职责边界：
 * <ul>
 *   <li><strong>认证（Authentication）</strong>：对携带 {@code Authorization: Bearer} 的请求调用
 *       {@code /oauth2/introspect}，得到令牌是否有效及用户身份（标准 OAuth2 资源服务器语义）</li>
 *   <li><strong>授权（Authorization）—仅“是否已登录”</strong>：当前配置为
 *       {@code .anyExchange().authenticated()}，即只要求“已通过不透明令牌校验”，
 *       <strong>不做</strong>方法级 {@code @PreAuthorize} 那类细粒度权限判定（细粒度在下游微服务）</li>
 *   <li><strong>异常响应</strong>：未认证 → 401；Spring Security 判定“已认证但拒绝访问” → 403，
 *       两套入口分别由 {@link GatewayAuthenticationEntryPoint} 与 {@link GatewayAccessDeniedHandler} 统一 JSON</li>
 * </ul>
 * <p>
 * 为什么不把“查 Redis 是否存在 token”放在网关：网关与认证中心存储解耦，跨进程读对方 Redis 会破坏边界、
 * 暴露凭据与一致性风险；标准做法是以自省端点为唯一校验入口（可再配合短 TTL 缓存，见 03 规则）。
 *
 * @author Fu Wei
 */
@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
@EnableConfigurationProperties({OAuth2ResourceServerProperties.class, GatewaySecurityProperties.class})
@RequiredArgsConstructor
public class GatewaySecurityConfiguration {

    /**
     * 白名单等网关自定义安全参数（如 {@code gateway.security.white-list}）
     */
    private final GatewaySecurityProperties gatewaySecurityProperties;

    /**
     * 已认证但访问被拒绝时的 JSON 响应体（典型为 403，与 {@link #authenticationEntryPoint} 的 401 区分）
     */
    private final GatewayAccessDeniedHandler accessDeniedHandler;

    /**
     * 未认证或令牌缺失/失效时的 JSON 响应体（401 + {@code WWW-Authenticate: Bearer}）
     */
    private final GatewayAuthenticationEntryPoint authenticationEntryPoint;

    /**
     * Boot 托管的 {@code spring.security.oauth2.resourceserver.opaquetoken.*}，自省 URI 与客户端凭证由此读取
     */
    private final OAuth2ResourceServerProperties oauth2ResourceServerProperties;

    /**
     * 注册请求头治理过滤器 Bean：清理伪造内部头、注入 TraceId / 起始时间（order 早于用户上下文过滤器）
     *
     * @return 全局过滤器实例
     */
    @Bean
    public RequestHeaderGovernanceGlobalFilter requestHeaderGovernanceGlobalFilter() {
        return new RequestHeaderGovernanceGlobalFilter();
    }

    /**
     * 构建网关唯一一条 {@link SecurityWebFilterChain}：不透明令牌自省 + 白名单 + OAuth2 资源服务器行为
     * <p>
     * 与下游 {@code sca-skeleton-starter-security} 对齐点：均使用不透明令牌 + 自省 + 将 {@code permissions}
     * 转为 {@code GrantedAuthority}（此处为响应式 {@link PermissionsReactiveOpaqueTokenAuthenticationConverter}）。
     *
     * @param http ServerHttpSecurity 建造器，由 Spring 注入
     * @return 反应式安全过滤链
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        // 从统一配置读取 opaque 自省三要素，缺失则启动失败，避免运行期才暴露配置错误
        OAuth2ResourceServerProperties.Opaquetoken opaque = oauth2ResourceServerProperties.getOpaquetoken();
        if (!StringUtils.hasText(opaque.getIntrospectionUri())
            || !StringUtils.hasText(opaque.getClientId())
            || !StringUtils.hasText(opaque.getClientSecret())) {
            throw new IllegalStateException(
                "网关需配置 spring.security.oauth2.resourceserver.opaquetoken "
                    + "(introspection-uri, client-id, client-secret)");
        }
        // 官方构造的响应式自省器：向 auth 的 /oauth2/introspect 发起校验（网关作为资源服务器客户端）
        ReactiveOpaqueTokenIntrospector introspector = SpringReactiveOpaqueTokenIntrospector
            .withIntrospectionUri(opaque.getIntrospectionUri())
            .clientId(opaque.getClientId())
            .clientSecret(opaque.getClientSecret())
            .build();

        // 白名单路径（登录、验证码、健康检查等）免 Bearer 校验，其余路径必须携带有效 access_token
        String[] whiteList = gatewaySecurityProperties.getWhiteList().toArray(new String[0]);

        http
            // 无状态 API：禁用 CSRF；不做浏览器表单登录与 HTTP Basic
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                // 白名单：permitAll，不构造认证对象
                .pathMatchers(whiteList).permitAll()
                // 非白名单：仅要求 authenticated（= 令牌自省通过），不在此链做接口级权限表达式
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .opaqueToken(opaqueToken -> opaqueToken
                    // 自省器 + 将 permissions claim 映射为权限，供将来若在网关增加路径级授权时使用
                    .introspector(introspector)
                    .authenticationConverter(new PermissionsReactiveOpaqueTokenAuthenticationConverter()))
                // 令牌问题走 401 入口；权限不足走 403（当前以 authenticated 为主，403 多见于后续扩展路径规则时）
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            // 非 oauth2 子系统抛出的认证/鉴权异常同样走统一 JSON 处理
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            );

        return http.build();
    }
}
