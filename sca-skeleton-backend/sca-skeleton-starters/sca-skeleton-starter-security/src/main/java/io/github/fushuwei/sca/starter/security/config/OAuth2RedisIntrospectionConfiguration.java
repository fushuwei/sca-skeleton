package io.github.fushuwei.sca.starter.security.config;

import io.github.fushuwei.sca.oauth2.redis.OAuth2AuthorizationRedisReader;
import io.github.fushuwei.sca.starter.security.introspection.RedisOpaqueTokenIntrospector;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

/**
 * Redis 不透明令牌自省相关 Bean 注册。
 * <p>
 * 当 classpath 存在 {@link StringRedisTemplate} 时注册 {@link OAuth2AuthorizationRedisReader}
 * 与 {@link RedisOpaqueTokenIntrospector}，供 {@link ResourceServerAutoConfiguration} 注入。
 *
 * @author Fu Wei
 */
@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
public class OAuth2RedisIntrospectionConfiguration {

    /**
     * 注册 Redis 授权只读访问器。
     *
     * @param stringRedisTemplate 与认证中心共用的 Redis 模板
     * @return 只读访问器
     */
    @Bean
    @ConditionalOnMissingBean
    public OAuth2AuthorizationRedisReader oauth2AuthorizationRedisReader(StringRedisTemplate stringRedisTemplate) {
        return new OAuth2AuthorizationRedisReader(stringRedisTemplate);
    }

    /**
     * 注册基于 Redis 的 {@link OpaqueTokenIntrospector} 实现。
     *
     * @param authorizationRedisReader 授权读模型
     * @return Redis 自省器
     */
    @Bean
    @ConditionalOnMissingBean(OpaqueTokenIntrospector.class)
    public OpaqueTokenIntrospector redisOpaqueTokenIntrospector(OAuth2AuthorizationRedisReader authorizationRedisReader) {
        return new RedisOpaqueTokenIntrospector(authorizationRedisReader);
    }
}
