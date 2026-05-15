package io.github.fushuwei.scaskeleton.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义网关安全配置属性
 *
 * @author Fu Wei
 */
@Data
@ConfigurationProperties(prefix = GatewaySecurityProperties.PREFIX)
public class GatewaySecurityProperties {

    public static final String PREFIX = "gateway.security";

    /**
     * 白名单路径列表，这些路径无需携带 Access Token 即可访问
     */
    private List<String> whiteList = new ArrayList<>();
}
