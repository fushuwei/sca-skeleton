package io.github.fushuwei.sca.gateway.config;

import io.github.fushuwei.sca.gateway.handler.GatewayAccessDeniedHandler;
import io.github.fushuwei.sca.gateway.handler.GatewayAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtGrantedAuthoritiesConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.util.AntPathMatcher;

/**
 * 网关响应式安全配置。
 * <p>
 * 职责：
 * <ol>
 *   <li>白名单路径直接放行（无需 JWT）</li>
 *   <li>其余请求验证 JWT 有效性（签名、有效期、issuer）</li>
 *   <li>将 JWT scope/permissions 映射为 GrantedAuthority</li>
 *   <li>自定义 401/403 响应（JSON 格式，保持与下游服务统一）</li>
 * </ol>
 * <p>
 * 注意：网关仅做 JWT 合法性验证，细粒度鉴权由下游资源服务负责。
 *
 * @author Fu Wei
 */
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class GatewaySecurityConfig {

    private final GatewayProperties gatewayProperties;
    private final GatewayAccessDeniedHandler accessDeniedHandler;
    private final GatewayAuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        // 动态构建白名单数组
        String[] whiteList = gatewayProperties.getWhiteList().toArray(new String[0]);

        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(whiteList).permitAll()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            );

        return http.build();
    }

    /**
     * JWT 认证转换器：将 JWT 中的 permissions claim 映射为 Spring Security GrantedAuthority。
     * 权限码格式与授权服务器写入的 permissions claim 一致（如 sys:user:list）。
     */
    private ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        ReactiveJwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
                new ReactiveJwtGrantedAuthoritiesConverter();
        // 从 permissions claim 读取权限（不加 SCOPE_ 前缀）
        grantedAuthoritiesConverter.setAuthoritiesClaimName("permissions");
        grantedAuthoritiesConverter.setAuthorityPrefix("");

        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return converter;
    }
}
