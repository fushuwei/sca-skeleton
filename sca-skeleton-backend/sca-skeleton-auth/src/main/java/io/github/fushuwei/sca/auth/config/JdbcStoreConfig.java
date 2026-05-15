package io.github.fushuwei.sca.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import javax.sql.DataSource;

/**
 * SAS JDBC 持久化配置。
 * <p>
 * 将注册客户端与 consent 绑定到数据库，表结构见 deploy/sql/install/sca_platform.sql：
 * {@code oauth2_registered_client}、{@code oauth2_authorization_consent}。
 * 授权记录 {@code oauth2_authorization} 由 {@link RedisOAuth2AuthorizationConfig} 写入 Redis，不再使用本配置持久化。
 *
 * @author Fu Wei
 */
@Configuration(proxyBeanMethods = false)
public class JdbcStoreConfig {

    @Bean
    public RegisteredClientRepository registeredClientRepository(DataSource dataSource) {
        return new JdbcRegisteredClientRepository(new JdbcTemplate(dataSource));
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
            DataSource dataSource,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(
                new JdbcTemplate(dataSource), registeredClientRepository);
    }
}
