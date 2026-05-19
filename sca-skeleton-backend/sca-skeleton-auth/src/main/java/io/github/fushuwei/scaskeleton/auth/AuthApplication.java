package io.github.fushuwei.scaskeleton.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 认证服务启动类。
 * <p>
 * 基于 Spring Authorization Server starter（由 Spring Boot 4 内置工件
 * {@code spring-boot-starter-security-oauth2-authorization-server} 提供）实现 OAuth2 授权服务。
 * 对外暴露标准端点：
 * <ul>
 *   <li>{@code POST /oauth2/token}：颁发不透明 access_token 与 refresh_token（含自定义 password 授权）</li>
 *   <li>{@code POST /oauth2/introspect}：资源服务器校验不透明 access_token，返回业务 claims</li>
 *   <li>{@code POST /oauth2/revoke}：吊销 access_token 或 refresh_token</li>
 *   <li>{@code GET  /oauth2/jwks}：发布 JWK（仅用于 OIDC id_token 等 JWT 场景）</li>
 * </ul>
 * 授权会话（含不透明令牌索引）持久化到 Redis；注册客户端与 consent 持久化到 MySQL。
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
