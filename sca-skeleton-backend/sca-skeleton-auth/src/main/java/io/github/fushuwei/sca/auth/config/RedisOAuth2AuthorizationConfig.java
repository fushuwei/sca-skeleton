package io.github.fushuwei.sca.auth.config;

import io.github.fushuwei.sca.auth.authorization.RedisOAuth2AuthorizationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * OAuth2 授权记录 Redis 存储配置。
 * <p>
 * 将 {@link OAuth2AuthorizationService} 绑定为 {@link RedisOAuth2AuthorizationService}，
 * 运行时授权会话（含不透明访问令牌索引）存放在 Redis；{@link JdbcStoreConfig} 仍负责注册客户端与 consent 的 JDBC 存储。
 *
 * @author Fu Wei
 */
@Configuration
public class RedisOAuth2AuthorizationConfig {

    /**
     * 注册基于 Redis 的授权服务：依赖 {@link StringRedisTemplate} 与 {@link RegisteredClientRepository}。
     *
     * @param registeredClientRepository JDBC 注册客户端仓库
     * @param stringRedisTemplate        Spring Boot 自动配置的字符串 Redis 模板
     * @return Redis 实现的 {@link OAuth2AuthorizationService}
     */
    @Bean
    public OAuth2AuthorizationService authorizationService(
            RegisteredClientRepository registeredClientRepository,
            StringRedisTemplate stringRedisTemplate) {
        return new RedisOAuth2AuthorizationService(registeredClientRepository, stringRedisTemplate);
    }
}
