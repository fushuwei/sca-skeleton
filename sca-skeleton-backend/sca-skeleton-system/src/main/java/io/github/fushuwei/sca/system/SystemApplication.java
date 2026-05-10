package io.github.fushuwei.sca.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 系统管理服务启动类。
 * <p>
 * 提供用户、角色、权限、部门、岗位、字典、配置等基础管理功能，
 * 是整个平台的权限数据来源，认证服务通过 JWT Claims 从本服务授权结果中获取信息。
 *
 * @author Fu Wei
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("io.github.fushuwei.sca.system.infrastructure.mapper")
public class SystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemApplication.class, args);
    }
}
