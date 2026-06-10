package io.github.fushuwei.scaskeleton.security.config;

import io.github.fushuwei.scaskeleton.security.introspection.RedisOpaqueTokenIntrospector;
import io.github.fushuwei.scaskeleton.security.oauth2.authorization.RedisOAuth2AuthorizationService;
import io.github.fushuwei.scaskeleton.security.oauth2.client.RedisRegisteredClientRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

/**
 * OAuth2 授权 Redis 存储与本地不透明令牌自省自动配置。
 * <p>
 * 注册 {@link OAuth2AuthorizationService}、资源服务器侧只读 {@link RegisteredClientRepository}（无 JDBC 时）、
 * 以及基于 {@link OAuth2AuthorizationService#findByToken} 的 {@link OpaqueTokenIntrospector}。
 *
 * @author Fu Wei
 */
@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
public class OAuth2AuthorizationRedisAutoConfiguration {

    /**
     * 资源服务器进程无 JDBC 客户端仓库时，提供 Redis 只读实现。
     *
     * @param stringRedisTemplate Redis 模板
     * @return 只读注册客户端仓库
     */
    @Bean
    @ConditionalOnMissingBean(RegisteredClientRepository.class)
    public RegisteredClientRepository redisRegisteredClientRepository(StringRedisTemplate stringRedisTemplate) {
        // 无 JDBC 时由 Redis 快照只读加载客户端
        return new RedisRegisteredClientRepository(stringRedisTemplate);
    }

    /**
     * 注册 Redis 版 {@link OAuth2AuthorizationService}，认证中心与资源服务器共用。
     *
     * @param registeredClientRepository 注册客户端仓库
     * @param stringRedisTemplate        Redis 模板
     * @return 授权服务
     */
    @Bean
    @ConditionalOnMissingBean(OAuth2AuthorizationService.class)
    public OAuth2AuthorizationService redisOAuth2AuthorizationService(
            RegisteredClientRepository registeredClientRepository,
            StringRedisTemplate stringRedisTemplate) {
        // 认证中心与资源服务器共用 Redis 授权存储
        return new RedisOAuth2AuthorizationService(registeredClientRepository, stringRedisTemplate);
    }

    /**
     * 注册基于 Redis 授权服务的 {@link OpaqueTokenIntrospector}。
     *
     * @param authorizationService OAuth2 授权服务
     * @return Redis 自省器
     */
    @Bean
    @ConditionalOnMissingBean(OpaqueTokenIntrospector.class)
    public OpaqueTokenIntrospector redisOpaqueTokenIntrospector(OAuth2AuthorizationService authorizationService) {
        // 基于 findByToken 的本地自省，不走 HTTP /oauth2/introspect
        return new RedisOpaqueTokenIntrospector(authorizationService);
    }
}
