package io.github.fushuwei.sca.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API 网关启动类
 * <p>
 * 基于 Spring Cloud Gateway（响应式 WebFlux），承担以下职责：
 * <ul>
 *   <li>JWT 签名验证（从认证服务 /oauth2/jwks 动态获取公钥）</li>
 *   <li>白名单路径放行（登录、验证码、JWK Set 端点等）</li>
 *   <li>用户上下文注入（将 JWT Claims 转为 X-User-Id / X-Username / X-Tenant-Id 请求头）</li>
 *   <li>统一路由转发至下游微服务</li>
 * </ul>
 *
 * @author Fu Wei
 */
@EnableDiscoveryClient
@SpringBootApplication
public class GatewayApplication {

    static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
