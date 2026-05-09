package io.github.fushuwei.sca.starter.security.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Security Starter 配置属性。
 * <p>
 * 通过 {@code sca.security.*} 在 {@code application.yml} 中配置，例如：
 * <pre>
 * sca:
 *   security:
 *     # JWT 签发方地址（认证服务），资源服务通过 /.well-known/openid-configuration 获取公钥
 *     issuer-uri: http://auth-service:9000
 *     # 白名单路径，不需要认证即可访问
 *     permit-paths:
 *       - /actuator/**
 *       - /v3/api-docs/**
 *       - /captcha/**
 *       - /auth/login
 * </pre>
 *
 * @author Fu Wei
 */
@Data
@ConfigurationProperties(prefix = "sca.security")
public class SecurityProperties {

    /**
     * JWT 签发方地址（认证服务 issuer-uri）。
     * 资源服务将从该地址的 /.well-known/openid-configuration 端点获取 JWK Set 公钥。
     */
    private String issuerUri;

    /**
     * JWK Set URI，可直接指定公钥端点（优先级高于 issuerUri 自动推导）。
     * 适用于不完全实现 OIDC Discovery 的认证服务。
     */
    private String jwkSetUri;

    /**
     * 免认证路径白名单，支持 Ant 风格匹配。
     * 默认包含 actuator 监控、OpenAPI 文档、验证码等基础路径。
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
     * JWT Claims 中存储用户 ID 的字段名，默认为 "sub"（标准 JWT Subject 字段）。
     * 若认证服务使用自定义字段名（如 "userId"），可在此处配置。
     */
    private String userIdClaimName = "sub";

    /**
     * JWT Claims 中存储用户名的字段名，默认为 "preferred_username"（OIDC 标准字段）。
     */
    private String usernameClaimName = "preferred_username";
}
