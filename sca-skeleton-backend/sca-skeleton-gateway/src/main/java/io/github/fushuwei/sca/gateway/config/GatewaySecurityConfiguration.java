package io.github.fushuwei.sca.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * 网关安全：对转发到下游的请求做 JWT 校验，认证相关路径放行。
 *
 * @author Fu Wei
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfiguration {

    // 响应式安全链，issuer 由配置 `spring.security.oauth2.resourceserver.jwt.issuer-uri` 指定。
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        // 登录、协议与元数据端点无需访问令牌。
        http.authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .pathMatchers(
                                "/oauth2/**",
                                "/.well-known/**",
                                "/login/**",
                                "/userinfo")
                        .permitAll()
                        .anyExchange()
                        .authenticated())
                // 使用 JWT 资源服务器默认配置拉取 JWK。
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                // 网关为无状态 API 边界，关闭 CSRF。
                .csrf(ServerHttpSecurity.CsrfSpec::disable);
        return http.build();
    }
}
