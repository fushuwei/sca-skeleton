package io.github.fushuwei.scaskeleton.security.oauth2.authorization;

import io.github.fushuwei.scaskeleton.security.user.jackson.ScaUserDetailsJacksonModule;
import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.json.JsonMapper;

/**
 * OAuth2 授权服务器相关 JSON 序列化器工厂。
 * <p>
 * 与 SAS {@code JdbcOAuth2AuthorizationService.Jackson3} / {@code JdbcRegisteredClientRepository.Jackson3}
 * 使用相同的 Jackson 3 模块发现方式，保证 {@link org.springframework.security.oauth2.server.authorization.OAuth2Authorization}
 * 的 attributes / token metadata 等 Map 字段可正确往返 Redis。
 * 注册客户端缓存请使用 {@link RegisteredClientRedisSerializer}，勿整对象序列化 {@code RegisteredClient}。
 *
 * @author Fu Wei
 */
public final class OAuth2AuthorizationJsonMapperFactory {

    private OAuth2AuthorizationJsonMapperFactory() {
    }

    /**
     * 创建用于 OAuth2 授权与注册客户端 Redis 持久化的 {@link JsonMapper}。
     * <p>
     * {@link SecurityJacksonModules#getModules(ClassLoader)} 在 classpath 存在 SAS 时会自动注册
     * {@code OAuth2AuthorizationServerJacksonModule}，无需也不应再显式引入 jackson2 包下的模块。
     *
     * @param classLoader 加载 Security Jackson 模块的类加载器
     * @return 配置完成的 JsonMapper
     */
    public static JsonMapper create(ClassLoader classLoader) {
        BasicPolymorphicTypeValidator.Builder typeValidatorBuilder = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("io.github.fushuwei.scaskeleton.security.user");
        return JsonMapper.builder()
                .addModules(SecurityJacksonModules.getModules(classLoader, typeValidatorBuilder))
                .addModule(new ScaUserDetailsJacksonModule())
                .build();
    }
}
