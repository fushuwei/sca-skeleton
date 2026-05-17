package io.github.fushuwei.scaskeleton.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API 网关启动类
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
