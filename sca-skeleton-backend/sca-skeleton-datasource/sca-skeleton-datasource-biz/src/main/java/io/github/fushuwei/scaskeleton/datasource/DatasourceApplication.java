package io.github.fushuwei.scaskeleton.datasource;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 数据源管理服务启动类。
 * <p>
 * 业务定位：数据源管理、驱动管理、SQL 查询三大能力；管理 MySQL、Oracle、PostgreSQL、SQLServer、
 * 达梦、人大金仓、MongoDB、ClickHouse、OceanBase、GaussDB 共 10 类数据库。
 * <p>
 * 安全模型：本服务以 OAuth2 资源服务器角色运行，通过 Redis 本地自省校验不透明 access_token，
 * 并基于 introspect 响应中的 authorities 配合 @RequiresPermission 完成方法级权限控制。
 * <p>
 * 引擎架构：本模块额外引入 engine 基础设施包（Rule-01 例外声明），承载驱动类加载隔离、
 * 动态连接池、方言适配、JDBC 引擎等基础设施能力，不属于 MVC 分层。
 *
 * @author Fu Wei
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("io.github.fushuwei.scaskeleton.datasource.mapper")
public class DatasourceApplication {

    static void main(String[] args) {
        // 启动 Spring Boot 应用：注册 Nacos 服务发现、扫描 MyBatis Mapper、加载资源服务器安全配置
        SpringApplication.run(DatasourceApplication.class, args);
    }
}
