package io.github.fushuwei.scaskeleton.auth.config;

import io.github.fushuwei.scaskeleton.security.oauth2.client.RedisRegisteredClientRepository;
import io.github.fushuwei.scaskeleton.security.oauth2.authorization.RedisOAuth2AuthorizationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
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
 * 授权记录 {@code oauth2_authorization} 由 {@code starter-security} 中的
 * {@link RedisOAuth2AuthorizationService} 写入 Redis。
 *
 * @author Fu Wei
 */
@Configuration(proxyBeanMethods = false)
public class JdbcStoreConfig {

    /**
     * JDBC 注册客户端仓库，写入时同步缓存到 Redis 供资源服务器只读加载。
     *
     * @param dataSource          数据源
     * @param stringRedisTemplate Redis 模板
     * @return 带 Redis 缓存的客户端仓库
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(DataSource dataSource,
            StringRedisTemplate stringRedisTemplate) {
        // JDBC 为主存储：读写 oauth2_registered_client 表
        JdbcRegisteredClientRepository jdbc = new JdbcRegisteredClientRepository(new JdbcTemplate(dataSource));
        // 装饰器同步写入 Redis，供资源服务器只读加载 client 配置
        return new RedisRegisteredClientRepository(jdbc, stringRedisTemplate);
    }

    /**
     * JDBC 授权确认（consent）持久化。
     *
     * @param dataSource                  数据源
     * @param registeredClientRepository  注册客户端仓库
     * @return consent 服务
     */
    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
            DataSource dataSource,
            RegisteredClientRepository registeredClientRepository) {
        // consent 仍走 JDBC，与 SAS 官方表 oauth2_authorization_consent 对齐
        return new JdbcOAuth2AuthorizationConsentService(
                new JdbcTemplate(dataSource), registeredClientRepository);
    }
}
