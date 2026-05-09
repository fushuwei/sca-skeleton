package io.github.fushuwei.sca.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 认证服务可调参数，集中读取 application 配置。
 *
 * @author Fu Wei
 */
@Data
@ConfigurationProperties(prefix = "sca.auth")
public class ScaAuthProperties {

    // JWT issuer，需与网关校验配置一致。
    private String issuer = "http://127.0.0.1:9000";
    // 访问令牌有效期秒。
    private long accessTokenTtlSeconds = 3600;
    // 刷新令牌有效期秒。
    private long refreshTokenTtlSeconds = 604800;
}
