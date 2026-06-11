package io.github.fushuwei.scaskeleton.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * OAuth2 资源服务器安全配置属性类
 *
 * @author Fu Wei
 */
@Data
@ConfigurationProperties(prefix = "sca.security")
public class OAuth2ResourceServerProperties {

    /**
     * 免认证路径白名单，支持 Ant 风格匹配
     * 默认包含 actuator 监控、OpenAPI 文档、验证码等基础路径
     */
    private List<String> permitPaths = new ArrayList<>();
}
