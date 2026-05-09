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

/**
 * OAuth2 持久化组件，与 DDL 中 oauth2_* 表一一对应。
 *
 * @author Fu Wei
 */
@Configuration
public class OAuth2StoreConfiguration {

    // JDBC 客户端仓库，读写 oauth2_registered_client。
    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        // 使用 Spring Security 自带 JDBC 实现，字段与官方 schema 对齐。
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    // JDBC 授权记录服务，读写 oauth2_authorization。
    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        // 将授权码与令牌授权过程落库，便于集群一致性与审计。
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    // JDBC 授权确认服务，读写 oauth2_authorization_consent。
    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        // 记录用户已确认的 scope，减少重复授权交互。
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }
}
