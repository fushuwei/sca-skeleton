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
 * 网关安全配置类
 *
 * @author Fu Wei
 */
@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
@EnableConfigurationProperties({OAuth2ResourceServerProperties.class, GatewaySecurityProperties.class})
@RequiredArgsConstructor
public class GatewaySecurityConfiguration {

    /**
     * 自定义网关安全配置属性
     */
    private final GatewaySecurityProperties gatewaySecurityProperties;

    /**
     * 403 处理器
     */
    private final GatewayAccessDeniedHandler accessDeniedHandler;

    /**
     * 401 入口
     */
    private final GatewayAuthenticationEntryPoint authenticationEntryPoint;

    /**
     * Boot 标准 OAuth2 资源服务器属性（opaque introspection）
     */
    private final OAuth2ResourceServerProperties oauth2ResourceServerProperties;

    /**
     * 请求头治理全局过滤器
     */
    @Bean
    public RequestHeaderGovernanceGlobalFilter requestHeaderGovernanceGlobalFilter() {
        return new RequestHeaderGovernanceGlobalFilter();
    }

    /**
     * 配置安全过滤链：自省、白名单、异常响应
     *
     * @param http ServerHttpSecurity
     * @return SecurityWebFilterChain
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        OAuth2ResourceServerProperties.Opaquetoken opaque = oauth2ResourceServerProperties.getOpaquetoken();
        if (!StringUtils.hasText(opaque.getIntrospectionUri())
            || !StringUtils.hasText(opaque.getClientId())
            || !StringUtils.hasText(opaque.getClientSecret())) {
            throw new IllegalStateException(
                "网关需配置 spring.security.oauth2.resourceserver.opaquetoken "
                    + "(introspection-uri, client-id, client-secret)");
        }
        ReactiveOpaqueTokenIntrospector introspector = new SpringReactiveOpaqueTokenIntrospector(
            opaque.getIntrospectionUri(), opaque.getClientId(), opaque.getClientSecret());

        String[] whiteList = gatewaySecurityProperties.getWhiteList().toArray(new String[0]);

        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(whiteList).permitAll()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .opaqueToken(opaqueToken -> opaqueToken
                    .introspector(introspector)
                    .authenticationConverter(new PermissionsReactiveOpaqueTokenAuthenticationConverter()))
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            );

        return http.build();
    }
}
