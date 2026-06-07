package io.github.fushuwei.scaskeleton.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 认证服务启动类。
 * <p>
 * 承担 OAuth2 授权服务器职责：令牌颁发、登录认证、客户端注册与 Redis 授权持久化。
 * 通过 Nacos 注册为 {@code sca-skeleton-auth}，对外暴露 {@code /oauth2/*} 标准端点。
 *
 * @author Fu Wei
 */
@MapperScan("io.github.fushuwei.scaskeleton.auth.infrastructure.mapper")
@EnableDiscoveryClient
@SpringBootApplication
public class AuthApplication {

    public static void main(String[] args) {
        // 启动 Spring Boot 应用并注册到服务发现
        SpringApplication.run(AuthApplication.class, args);
    }
}
