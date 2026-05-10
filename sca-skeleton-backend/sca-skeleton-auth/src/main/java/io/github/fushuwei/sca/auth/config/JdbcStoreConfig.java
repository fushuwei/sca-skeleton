package io.github.fushuwei.sca.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import javax.sql.DataSource;

/**
 * SAS JDBC 持久化配置。
 * <p>
 * 将三个核心存储对象绑定到数据库，表结构见 deploy/sql/install/sca_platform.sql：
 * oauth2_registered_client / oauth2_authorization / oauth2_authorization_consent。
 *
 * @author Fu Wei
 */
@Configuration
public class JdbcStoreConfig {

    @Bean
    public RegisteredClientRepository registeredClientRepository(DataSource dataSource) {
        return new JdbcRegisteredClientRepository(new JdbcTemplate(dataSource));
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(
            DataSource dataSource,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(
                new JdbcTemplate(dataSource), registeredClientRepository);
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
            DataSource dataSource,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(
                new JdbcTemplate(dataSource), registeredClientRepository);
    }
}
