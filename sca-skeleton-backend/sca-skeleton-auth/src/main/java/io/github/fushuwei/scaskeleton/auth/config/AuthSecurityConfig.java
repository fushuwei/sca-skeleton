package io.github.fushuwei.scaskeleton.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Auth 服务默认安全过滤链：仅放行 Actuator、验证码和 API 文档。
 * <p>
 * 密码模式下，登录 UI 由前端 SPA 提供，auth 服务不再渲染 Thymeleaf 登录页。
 * OAuth2 token 端点由 {@link AuthorizationServerConfig} 的 Order=1 过滤链处理。
 *
 * @author Fu Wei
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class AuthSecurityConfig {

    /**
     * 默认安全过滤链（Order=2，低于 SAS 端点链）。
     * <p>
     * 放行 Actuator 健康检查、验证码生成接口、API 文档和静态资源，
     * 其余请求需要认证（由资源服务器 SecurityFilterChain 处理）。
     *
     * @param http HttpSecurity
     * @return Order=2 的 FilterChain
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                // 健康检查无需认证
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // 验证码生成接口放行（前端经网关访问，到达 auth 服务时路径为 /captcha/**）
                .requestMatchers("/captcha/**").permitAll()
                // API 文档放行
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/webjars/**").permitAll()
                // 其余请求需认证
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
