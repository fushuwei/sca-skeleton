package io.github.fushuwei.sca.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 认证服务启动类。
 * <p>
 * 基于 Spring Security 7（含 Spring Authorization Server）实现 OAuth2 授权服务，
 * 提供 {@code /oauth2/token}（自定义密码模式）颁发不透明访问令牌与刷新令牌；授权会话存 Redis。
 * 资源服务通过 {@code /oauth2/introspect} 校验访问令牌；JWK 仍可用于 OIDC id_token 等 JWT 场景。
 *
 * @author Fu Wei
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("io.github.fushuwei.sca.auth.infrastructure.mapper")
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
