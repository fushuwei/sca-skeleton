package io.github.fushuwei.sca.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API 网关入口：路由聚合、JWT 统一校验。
 *
 * @author Fu Wei
 */
@SpringBootApplication
public class GatewayApplication {

    // 启动 Spring Cloud Gateway 进程。
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
