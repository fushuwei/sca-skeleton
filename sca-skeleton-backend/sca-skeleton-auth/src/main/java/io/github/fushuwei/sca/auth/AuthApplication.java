package io.github.fushuwei.sca.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 认证授权服务启动类，负责 OAuth2 协议端点、登录与令牌签发。
 *
 * @author Fu Wei
 */
@SpringBootApplication
@MapperScan("io.github.fushuwei.sca.auth.infrastructure.mapper")
public class AuthApplication {

    // 启动 Spring Boot 应用入口。
    public static void main(String[] args) {
        // 交给 Spring Boot 引导上下文与自动配置。
        SpringApplication.run(AuthApplication.class, args);
    }
}
