package io.github.fushuwei.scaskeleton.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关安全配置属性类
 *
 * @author Fu Wei
 */
@Data
@ConfigurationProperties(prefix = GatewaySecurityProperties.PREFIX)
public class GatewaySecurityProperties {

    /**
     * 配置前缀：{@code gateway.security.*}
     */
    public static final String PREFIX = "gateway.security";

    /**
     * 白名单路径列表，Ant 风格匹配，命中后跳过 Bearer Token 校验，无需携带 Access Token 即可访问相关资源
     */
    private List<String> permitPaths = new ArrayList<>();
}
