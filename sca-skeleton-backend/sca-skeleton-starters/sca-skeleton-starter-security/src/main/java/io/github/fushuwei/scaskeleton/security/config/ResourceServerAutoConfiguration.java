package io.github.fushuwei.scaskeleton.security.config;

import io.github.fushuwei.scaskeleton.security.introspection.RedisOpaqueTokenIntrospector;
import tools.jackson.databind.ObjectMapper;
import io.github.fushuwei.scaskeleton.core.user.CurrentUserProvider;
import io.github.fushuwei.scaskeleton.security.authorization.RequiresPermissionAuthorizer;
import io.github.fushuwei.scaskeleton.security.handler.SecurityAccessDeniedHandler;
import io.github.fushuwei.scaskeleton.security.handler.SecurityAuthenticationEntryPoint;
import io.github.fushuwei.scaskeleton.security.introspection.PermissionsOpaqueTokenAuthenticationConverter;
import io.github.fushuwei.scaskeleton.security.properties.SecurityProperties;
import io.github.fushuwei.scaskeleton.security.user.CurrentUserProviderImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Resource Server 自动配置入口。
 * <p>
 * 配置内容：
 * <ol>
 *   <li>无状态 Session（Bearer 不透明令牌 + Redis 本地自省，无 Cookie/Session）</li>
 *   <li>禁用 CSRF（Bearer Token 场景无需 CSRF 防护）</li>
 *   <li>白名单路径免认证，其余路径均需有效 access_token</li>
 *   <li>令牌校验：默认注入 {@link RedisOpaqueTokenIntrospector}，
 *       直连认证中心 Redis，不走 HTTP {@code /oauth2/introspect}</li>
 *   <li>权限映射：{@link PermissionsOpaqueTokenAuthenticationConverter} 从 {@code permissions} 声明生成 GrantedAuthority</li>
 *   <li>统一 401/403 JSON 响应；403 由本服务 {@code @PreAuthorize} / {@code @RequiresPermission} 触发</li>
 *   <li>开启方法级权限注解：{@code @PreAuthorize}、{@code @RequiresPermission}、{@code @Secured}</li>
 * </ol>
 *
 * @author Fu Wei
 */
@AutoConfiguration(after = OAuth2AuthorizationRedisAutoConfiguration.class)
@EnableMethodSecurity(securedEnabled = true)
@EnableConfigurationProperties(SecurityProperties.class)
@ConditionalOnProperty(prefix = "sca.security.resource-server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ResourceServerAutoConfiguration {

    /**
     * 注册 {@link RequiresPermission} 的 SpEL 校验 Bean（固定 bean 名供元注解引用）。
     *
     * @return 权限校验委托器
     */
    @Bean(name = "requiresPermissionAuthorizer")
    @ConditionalOnMissingBean(RequiresPermissionAuthorizer.class)
    public RequiresPermissionAuthorizer requiresPermissionAuthorizer() {
        // 供 @RequiresPermission 元注解 @PreAuthorize SpEL 引用
        return new RequiresPermissionAuthorizer();
    }

    /**
     * 注册资源服务 SecurityFilterChain：Redis 不透明令牌自省、白名单、异常响应。
     *
     * @param http                        HttpSecurity
     * @param securityProperties          sca.security.*（白名单、claims 字段名）
     * @param opaqueTokenIntrospector     Redis 自省器（由 OAuth2RedisIntrospectionConfiguration 提供）
     * @param objectMapper                JSON 异常响应
     * @return SecurityFilterChain
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain resourceServerSecurityFilterChain(
            HttpSecurity http,
            SecurityProperties securityProperties,
            OpaqueTokenIntrospector opaqueTokenIntrospector,
            ObjectMapper objectMapper) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable);
        // 无 Session，Bearer 令牌无状态校验
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> {
            String[] permitPaths = securityProperties.getPermitPaths().toArray(String[]::new);
            // 配置白名单路径免认证
            if (permitPaths.length > 0) {
                auth.requestMatchers(permitPaths).permitAll();
            }
            // 其余路径需有效 access_token
            auth.anyRequest().authenticated();
        });

        // Redis 本地自省 + permissions 转 GrantedAuthority；401 返回统一 JSON
        http.oauth2ResourceServer(oauth2 -> oauth2
                .opaqueToken(opaqueToken -> opaqueToken
                        .introspector(opaqueTokenIntrospector)
                        .authenticationConverter(new PermissionsOpaqueTokenAuthenticationConverter()))
                .authenticationEntryPoint(new SecurityAuthenticationEntryPoint(objectMapper)));

        // 403 权限不足返回统一 JSON
        http.exceptionHandling(ex ->
                ex.accessDeniedHandler(new SecurityAccessDeniedHandler(objectMapper)));

        return http.build();
    }

    /**
     * 注册 CurrentUserProvider：从 BearerTokenAuthentication 的 token 属性读取用户信息。
     *
     * @param securityProperties 字段名配置
     * @return CurrentUserProviderImpl
     */
    @Bean
    @ConditionalOnMissingBean(CurrentUserProvider.class)
    public CurrentUserProvider currentUserProvider(SecurityProperties securityProperties) {
        // 从 BearerTokenAuthentication tokenAttributes 读取用户上下文
        return new CurrentUserProviderImpl(securityProperties);
    }
}
