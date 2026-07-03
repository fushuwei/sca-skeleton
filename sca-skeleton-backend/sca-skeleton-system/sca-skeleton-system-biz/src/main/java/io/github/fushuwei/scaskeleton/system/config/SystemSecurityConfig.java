package io.github.fushuwei.scaskeleton.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

/**
 * System 服务安全相关 Bean 配置。
 * <p>
 * 提供 PasswordEncoder Bean，用于用户创建/重置密码时的加密操作。
 * 与 auth 服务保持相同的 DelegatingPasswordEncoder 策略，
 * 确保 system 服务写入的密码哈希格式与 auth 服务解析格式兼容。
 *
 * @author Fu Wei
 */
@Configuration(proxyBeanMethods = false)
public class SystemSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 1. 创建 BCrypt 编码器，作为生产环境默认密码哈希算法
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        // 2. 组装 DelegatingPasswordEncoder，前缀 "bcrypt" 为默认匹配算法
        // 3. 同时注册 "noop" 适配器，便于本地开发读取 {noop} 前缀密文（与 auth 服务保持一致）
        return new DelegatingPasswordEncoder("bcrypt",
                Map.of("bcrypt", bcrypt,
                        "noop", org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance()));
    }
}
