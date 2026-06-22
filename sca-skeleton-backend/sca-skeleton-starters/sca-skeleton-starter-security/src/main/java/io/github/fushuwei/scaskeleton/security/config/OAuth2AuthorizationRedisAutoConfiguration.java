package io.github.fushuwei.scaskeleton.security.config;

import io.github.fushuwei.scaskeleton.security.introspection.RedisOpaqueTokenIntrospector;
import io.github.fushuwei.scaskeleton.security.oauth2.authorization.RedisOAuth2AuthorizationService;
import io.github.fushuwei.scaskeleton.security.oauth2.client.RedisRegisteredClientRepository;
import io.github.fushuwei.scaskeleton.security.oauth2.client.RegisteredClientRedisSerializer;
import io.github.fushuwei.scaskeleton.security.user.jackson.ScaUserDetailsJacksonModule;
import org.springframework.beans.factory.annotation.Qualifier;
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
     * OAuth2 持久层专用的 {@link JsonMapper}
     * <p>
     * Bean 名称为 {@code securityJsonMapper}，通过 {@code @Qualifier} 在注入时
     * 精确指定，与全局 {@code @Primary jsonMapper} 互不影响
     *
     * @return OAuth2 持久层专用 JsonMapper
     */
    @Bean
    @ConditionalOnMissingBean(name = "securityJsonMapper")
    public JsonMapper securityJsonMapper() {
        // 多态类型验证器：只允许指定包下的子类型进行多态反序列化，防止恶意类注入
        BasicPolymorphicTypeValidator.Builder typeValidatorBuilder = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("io.github.fushuwei.scaskeleton.security.user");

        // 构建 JsonMapper
        return JsonMapper.builder()
            .addModules(SecurityJacksonModules.getModules(getClass().getClassLoader(), typeValidatorBuilder))
            .addModule(new ScaUserDetailsJacksonModule())
            .build();
    }

    /**
     * 注册客户端 Redis 序列化器
     *
     * @param jsonMapper         全局通用 JsonMapper
     * @param securityJsonMapper OAuth2 持久层专用 JsonMapper
     * @return 注册客户端 Redis 序列化器
     */
    @Bean
    @ConditionalOnMissingBean(RegisteredClientRedisSerializer.class)
    public RegisteredClientRedisSerializer registeredClientRedisSerializer(JsonMapper jsonMapper,
                                                                           @Qualifier("securityJsonMapper") JsonMapper securityJsonMapper) {
        return new RegisteredClientRedisSerializer(jsonMapper, securityJsonMapper);
    }

    /**
     * 注册客户端存储库的 Redis 实现
     *
     * @param stringRedisTemplate Redis 字符串模板
     * @param redisSerializer     注册客户端 Redis 序列化器
     * @return 注册客户端存储库
     */
    @Bean
    @ConditionalOnMissingBean(RegisteredClientRepository.class)
    public RegisteredClientRepository redisRegisteredClientRepository(StringRedisTemplate stringRedisTemplate,
                                                                      RegisteredClientRedisSerializer redisSerializer) {
        return new RedisRegisteredClientRepository(stringRedisTemplate, redisSerializer);
    }

    /**
     * OAuth2 授权服务的 Redis 实现，替代 SAS 默认的 JDBC 存储
     *
     * @param registeredClientRepository 注册客户端存储库
     * @param stringRedisTemplate        Redis 字符串模板
     * @param securityJsonMapper         OAuth2 持久层专用 JsonMapper
     * @return OAuth2 授权服务
     */
    @Bean
    @ConditionalOnMissingBean(OAuth2AuthorizationService.class)
    public OAuth2AuthorizationService redisOAuth2AuthorizationService(RegisteredClientRepository registeredClientRepository,
                                                                      StringRedisTemplate stringRedisTemplate,
                                                                      @Qualifier("securityJsonMapper") JsonMapper securityJsonMapper) {
        return new RedisOAuth2AuthorizationService(registeredClientRepository,
            stringRedisTemplate, securityJsonMapper);
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
