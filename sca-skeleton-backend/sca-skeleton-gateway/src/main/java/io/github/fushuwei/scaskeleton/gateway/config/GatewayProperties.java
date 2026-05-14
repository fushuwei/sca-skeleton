package io.github.fushuwei.scaskeleton.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关自定义安全配置属性
 *
 * @author Fu Wei
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "gateway.security")
public class GatewayProperties {

    /**
     * 白名单路径列表，这些路径无需携带 Access Token 即可访问
     */
    private List<String> whiteList = new ArrayList<>();
}
