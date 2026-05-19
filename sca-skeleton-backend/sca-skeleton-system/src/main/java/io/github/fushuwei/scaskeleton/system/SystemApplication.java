package io.github.fushuwei.scaskeleton.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 系统管理服务启动类。
 * <p>
 * 业务定位：用户、角色、权限、部门、岗位、字典、配置等基础管理功能；同时作为整个平台的权限数据来源
 * （认证服务在颁发令牌时基于本服务的 RBAC 数据装配 {@code permissions} 业务 claim）。
 * <p>
 * 安全模型：本服务以 OAuth2 资源服务器角色运行，通过 {@code spring-boot-starter-oauth2-resource-server}
 * 调用认证服务 {@code /oauth2/introspect} 校验不透明 access_token，并基于 introspect 响应中的 permissions
 * 配合 {@code @RequiresPermission} 完成方法级权限控制。
 *
 * @author Fu Wei
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("io.github.fushuwei.scaskeleton.system.infrastructure.mapper")
public class SystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemApplication.class, args);
    }
}
