package io.github.fushuwei.scaskeleton.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 认证服务启动类
 *
 * @author Fu Wei
 */
@MapperScan("io.github.fushuwei.scaskeleton.auth.infrastructure.mapper")
@EnableDiscoveryClient
@SpringBootApplication
public class AuthApplication {

    static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
