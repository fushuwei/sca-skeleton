package io.github.fushuwei.sca.starter.security.oauth2;

import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import tools.jackson.databind.json.JsonMapper;

/**
 * OAuth2 授权服务器相关 JSON 序列化器工厂。
 * <p>
 * 与 SAS JDBC / Redis 实现使用一致的 Jackson 模块，保证 {@link org.springframework.security.oauth2.server.authorization.OAuth2Authorization}
 * 与 {@link org.springframework.security.oauth2.server.authorization.client.RegisteredClient} 可正确往返 Redis。
 *
 * @author Fu Wei
 */
public final class OAuth2AuthorizationJsonMapperFactory {

    private OAuth2AuthorizationJsonMapperFactory() {
    }

    /**
     * 创建用于 OAuth2 授权与注册客户端 Redis 持久化的 {@link JsonMapper}。
     *
     * @param classLoader 加载 Security Jackson 模块的类加载器
     * @return 配置完成的 JsonMapper
     */
    public static JsonMapper create(ClassLoader classLoader) {
        return JsonMapper.builder()
                .addModule(new OAuth2AuthorizationServerJackson2Module())
                .addModules(SecurityJacksonModules.getModules(classLoader))
                .build();
    }
}
