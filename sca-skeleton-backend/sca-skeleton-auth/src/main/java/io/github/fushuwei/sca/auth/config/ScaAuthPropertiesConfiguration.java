package io.github.fushuwei.sca.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

/**
 * 启用认证配置属性绑定。
 *
 * @author Fu Wei
 */
@Configuration
@EnableConfigurationProperties(ScaAuthProperties.class)
public class ScaAuthPropertiesConfiguration {

    // issuer 元数据，供 OAuth2 授权服务器自动配置与 JWK 发布使用。
    @Bean
    public AuthorizationServerSettings authorizationServerSettings(ScaAuthProperties authProperties) {
        // 与网关 resource server 的 issuer-uri 保持一致。
        return AuthorizationServerSettings.builder()
                .issuer(authProperties.getIssuer())
                .build();
    }
}
