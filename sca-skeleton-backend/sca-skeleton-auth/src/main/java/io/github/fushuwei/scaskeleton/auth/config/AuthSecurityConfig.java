package io.github.fushuwei.scaskeleton.auth.config;

import io.github.fushuwei.scaskeleton.auth.security.ScaUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Map;

/**
 * Spring Security 基础安全配置（默认过滤链，优先级低于授权服务器过滤链）。
 * <p>
 * 职责：
 * <ol>
 *   <li>放行公开端点（健康检查、验证码等）</li>
 *   <li>其余请求要求认证</li>
 *   <li>注册 PasswordEncoder（支持 {bcrypt} 前缀的 DelegatingPasswordEncoder）</li>
 * </ol>
 *
 * @author Fu Wei
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class AuthSecurityConfig {

    /**
     * 默认安全过滤链（Order=2，低于授权服务器过滤链）。
     * 处理所有非 SAS 端点的请求（如 actuator、自定义 API 等）。
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            ScaUserDetailsService userDetailsService) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize
                // 健康检查端点无需认证
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // 其余请求均需认证（授权服务器端点已由 Order=1 的链处理）
                .anyRequest().authenticated()
            )
            .userDetailsService(userDetailsService)
            // 表单登录（授权码模式的前端跳转登录页）
            .formLogin(form -> form.loginPage("/login").permitAll());

        return http.build();
    }

    /**
     * 密码编码器。
     * <p>
     * 使用 {@link DelegatingPasswordEncoder}，默认编码算法为 bcrypt，
     * 同时兼容 {noop}、{sha256} 等历史格式，适合存量数据迁移场景。
     * 数据库中密码字段格式示例：{bcrypt}$2a$10$...
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        DelegatingPasswordEncoder encoder = new DelegatingPasswordEncoder(
                "bcrypt", Map.of("bcrypt", bcrypt, "noop",
                org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance())
        );
        encoder.setDefaultPasswordEncoderForMatches(bcrypt);
        return encoder;
    }
}
