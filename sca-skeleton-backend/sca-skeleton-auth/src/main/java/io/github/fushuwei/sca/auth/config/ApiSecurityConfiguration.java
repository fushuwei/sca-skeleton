package io.github.fushuwei.sca.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 业务 API 安全链，登录与验证码走无会话状态。
 *
 * @author Fu Wei
 */
@Configuration
@EnableWebSecurity
public class ApiSecurityConfiguration {

    // 业务 API 安全链，优先级低于授权服务器链。
    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        // 登录与验证码接口放行，其余默认需认证以防误暴露管理端点。
        http.securityMatcher("/api/**")
                .authorizeHttpRequests(registry -> registry
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .anyRequest().authenticated())
                // 无状态会话，令牌由 OAuth2 与 JWT 承担。
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 本服务对外 API 关闭 CSRF，依赖令牌与同源策略。
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    // 委托密码编码器，兼容 {bcrypt} 等前缀格式。
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 使用 Spring Security 默认委托编码器，支持多种存储格式。
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // 暴露 AuthenticationManager 供登录服务调用。
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        // 使用配置阶段构建的 ProviderManager。
        return configuration.getAuthenticationManager();
    }
}
