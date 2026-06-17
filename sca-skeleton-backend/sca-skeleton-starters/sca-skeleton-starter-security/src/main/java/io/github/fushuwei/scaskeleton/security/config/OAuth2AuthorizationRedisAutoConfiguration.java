package io.github.fushuwei.scaskeleton.security.config;

import io.github.fushuwei.scaskeleton.security.introspection.RedisOpaqueTokenIntrospector;
import io.github.fushuwei.scaskeleton.security.oauth2.authorization.RedisOAuth2AuthorizationService;
import io.github.fushuwei.scaskeleton.security.oauth2.client.RedisRegisteredClientRepository;
import io.github.fushuwei.scaskeleton.security.user.jackson.ScaUserDetailsJacksonModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.json.JsonMapper;

/**
 * OAuth2 授权服务器 Redis 存储自动配置类
 *
 * @author Fu Wei
 */
@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
public class OAuth2AuthorizationRedisAutoConfiguration {

    /**
     * OAuth2 持久层专用的 {@link JsonMapper}，用于序列化/反序列化授权记录与注册客户端
     *
     * @return OAuth2 持久层专用 JsonMapper
     */
    @Bean
    @ConditionalOnMissingBean(name = "oauth2AuthorizationJsonMapper")
    public JsonMapper oauth2AuthorizationJsonMapper() {
        // 多态类型验证器：只允许指定包下的子类型进行多态反序列化， 防止恶意类注入
        BasicPolymorphicTypeValidator.Builder typeValidatorBuilder = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("io.github.fushuwei.scaskeleton.security.user");

        // 构建 JsonMapper
        return JsonMapper.builder()
            .addModules(SecurityJacksonModules.getModules(getClass().getClassLoader(), typeValidatorBuilder))
            .addModule(new ScaUserDetailsJacksonModule())
            .build();
    }

    /**
     * 注册客户端存储库的 Redis 实现
     *
     * @param stringRedisTemplate           Redis 字符串模板
     * @param oauth2AuthorizationJsonMapper OAuth2 持久层专用 JsonMapper
     * @return 注册客户端存储库
     */
    @Bean
    @ConditionalOnMissingBean(RegisteredClientRepository.class)
    public RegisteredClientRepository redisRegisteredClientRepository(StringRedisTemplate stringRedisTemplate,
                                                                      JsonMapper oauth2AuthorizationJsonMapper) {
        return new RedisRegisteredClientRepository(stringRedisTemplate, oauth2AuthorizationJsonMapper);
    }

    /**
     * OAuth2 授权服务的 Redis 实现，替代 SAS 默认的 JDBC 存储
     *
     * @param registeredClientRepository    注册客户端存储库
     * @param stringRedisTemplate           Redis 字符串模板
     * @param oauth2AuthorizationJsonMapper OAuth2 持久层专用 JsonMapper
     * @return OAuth2 授权服务
     */
    @Bean
    @ConditionalOnMissingBean(OAuth2AuthorizationService.class)
    public OAuth2AuthorizationService redisOAuth2AuthorizationService(RegisteredClientRepository registeredClientRepository,
                                                                      StringRedisTemplate stringRedisTemplate,
                                                                      JsonMapper oauth2AuthorizationJsonMapper) {
        return new RedisOAuth2AuthorizationService(registeredClientRepository,
            stringRedisTemplate, oauth2AuthorizationJsonMapper);
    }

    /**
     * OAuth2 授权服务的不透明令牌 Redis 本地自省器
     *
     * @param authorizationService OAuth2 授权服务
     * @return Redis 本地自省器，不走 /oauth2/introspect 端点
     */
    @Bean
    @ConditionalOnMissingBean(OpaqueTokenIntrospector.class)
    public OpaqueTokenIntrospector redisOpaqueTokenIntrospector(OAuth2AuthorizationService authorizationService) {
        return new RedisOpaqueTokenIntrospector(authorizationService);
    }
}
