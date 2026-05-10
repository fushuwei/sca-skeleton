package io.github.fushuwei.sca.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 认证服务启动类。
 * <p>
 * 基于 Spring Security 7（含 Spring Authorization Server）实现 OAuth2 授权服务，
 * 提供 {@code /oauth2/token} 端点（自定义密码授权模式）颁发 JWT 访问令牌与刷新令牌。
 * 令牌使用 RSA 非对称加密签名，公钥通过 {@code /oauth2/jwks} 端点对外暴露，
 * 供网关与各资源服务动态获取用于 JWT 验签。
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
