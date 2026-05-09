package io.github.fushuwei.sca.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 安全基础配置，提供统一口令编码器。
 *
 * @author Fu Wei
 */
@Configuration
@EnableWebSecurity
public class ApiSecurityConfiguration {

    // 委托密码编码器，兼容 {bcrypt} 等前缀格式。
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 使用 Spring Security 默认委托编码器，支持多种存储格式。
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
