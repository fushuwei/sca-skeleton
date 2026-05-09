package io.github.fushuwei.sca.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Actuator 端点专用安全链，避免落入默认全部拒绝策略。
 *
 * @author Fu Wei
 */
@Configuration
public class ActuatorSecurityConfiguration {

    // 健康与指标端点本地运维访问，生产应收紧网络或改为认证。
    @Bean
    @Order(3)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        // 仅匹配 Actuator 路径（Boot 4 不再提供 servlet EndpointRequest 时的等价写法）。
        http.securityMatcher("/actuator/**")
                // 全部放行以配合 K8s 探针，网络层需隔离。
                .authorizeHttpRequests(registry -> registry.anyRequest().permitAll())
                // Actuator 多为 GET/HEAD，关闭 CSRF 简化集成。
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
