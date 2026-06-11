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
 * 基于 Redis 实现的 OAuth2 授权自动配置类
 *
 * @author Fu Wei
 */
@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
public class OAuth2AuthorizationRedisAutoConfiguration {

    /**
     * 注册客户端存储库的 Redis 实现
     *
     * @param stringRedisTemplate Redis 字符串模板
     * @return 注册客户端存储库
     */
    @Bean
    @ConditionalOnMissingBean(RegisteredClientRepository.class)
    public RegisteredClientRepository redisRegisteredClientRepository(StringRedisTemplate stringRedisTemplate) {
        return new RedisRegisteredClientRepository(stringRedisTemplate);
    }

    /**
     * OAuth2 授权服务的 Redis 实现
     *
     * @param registeredClientRepository 注册客户端存储库
     * @param stringRedisTemplate        Redis 字符串模板
     * @return OAuth2 授权服务
     */
    @Bean
    @ConditionalOnMissingBean(OAuth2AuthorizationService.class)
    public OAuth2AuthorizationService redisOAuth2AuthorizationService(
        RegisteredClientRepository registeredClientRepository, StringRedisTemplate stringRedisTemplate) {
        return new RedisOAuth2AuthorizationService(registeredClientRepository, stringRedisTemplate);
    }

    /**
     * 基于 Redis 授权服务的 {@link OpaqueTokenIntrospector}
     *
     * @param authorizationService OAuth2 授权服务
     * @return Redis 本地自省器，不走 /oauth2/introspect 请求端点
     */
    @Bean
    @ConditionalOnMissingBean(OpaqueTokenIntrospector.class)
    public OpaqueTokenIntrospector redisOpaqueTokenIntrospector(OAuth2AuthorizationService authorizationService) {
        return new RedisOpaqueTokenIntrospector(authorizationService);
    }
}
