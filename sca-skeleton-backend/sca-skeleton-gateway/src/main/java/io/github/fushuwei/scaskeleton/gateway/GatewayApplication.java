package io.github.fushuwei.scaskeleton.gateway;

import io.github.fushuwei.scaskeleton.gateway.config.GatewaySecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关服务启动类
 *
 * @author Fu Wei
 */
@EnableDiscoveryClient
@SpringBootApplication
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class GatewayApplication {

    static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
