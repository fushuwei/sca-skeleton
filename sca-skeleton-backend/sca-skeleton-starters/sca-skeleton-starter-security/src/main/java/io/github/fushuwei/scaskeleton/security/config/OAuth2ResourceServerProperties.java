package io.github.fushuwei.scaskeleton.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * OAuth2 资源服务器配置属性类
 *
 * @author Fu Wei
 */
@Data
@ConfigurationProperties(prefix = "sca.security")
public class OAuth2ResourceServerProperties {

    /**
     * 免认证路径白名单，Ant 风格匹配，命中后跳过 Bearer Token 校验
     */
    private List<String> permitPaths = new ArrayList<>();
}
