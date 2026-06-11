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
     * 资源服务器（不透明令牌 Redis 自省、FilterChain）相关开关
     */
    private ResourceServer resourceServer = new ResourceServer();

    /**
     * 免认证路径白名单，支持 Ant 风格匹配
     * 默认包含 actuator 监控、OpenAPI 文档、验证码等基础路径
     */
    private List<String> permitPaths = new ArrayList<>(List.of(
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/doc.html",
            "/webjars/**",
            "/captcha/**"
    ));

    /**
     * 自省返回的 token 属性中用户 ID 字段名，默认 "sub"（与认证服务写入的 opaque claims 一致）
     */
    private String userIdClaimName = "sub";

    /**
     * 自省返回的 token 属性中用户名字段名，默认 "preferred_username"
     */
    private String usernameClaimName = "preferred_username";

    /**
     * 资源服务器自动配置开关；认证中心进程应设为 {@code false}，仅复用 OAuth2 Redis 存储 Bean
     */
    @Data
    public static class ResourceServer {

        /**
         * 是否启用资源服务器 SecurityFilterChain 与相关组件，业务微服务默认开启
         */
        private boolean enabled = true;
    }
}
