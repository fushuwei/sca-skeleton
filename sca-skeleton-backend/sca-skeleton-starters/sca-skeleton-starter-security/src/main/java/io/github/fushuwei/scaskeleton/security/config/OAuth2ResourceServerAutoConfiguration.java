package io.github.fushuwei.scaskeleton.security.config;

import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission;
import tools.jackson.databind.ObjectMapper;
import io.github.fushuwei.scaskeleton.core.user.CurrentUserProvider;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermissionAspect;
import io.github.fushuwei.scaskeleton.security.annotation.RequiresPermissionChecker;
import io.github.fushuwei.scaskeleton.security.handler.DefaultAccessDeniedHandler;
import io.github.fushuwei.scaskeleton.security.handler.DefaultAuthenticationEntryPoint;
import io.github.fushuwei.scaskeleton.security.introspection.DefaultOpaqueTokenAuthenticationConverter;
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
 * OAuth2 资源服务器自动配置类
 *
 * @author Fu Wei
 */
@AutoConfiguration(after = OAuth2AuthorizationRedisAutoConfiguration.class)
@EnableMethodSecurity
@EnableConfigurationProperties(OAuth2ResourceServerProperties.class)
@ConditionalOnProperty(prefix = "sca.security.resource-server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OAuth2ResourceServerAutoConfiguration {

    /**
     * 资源服务器安全过滤器链
     *
     * @param http                           HttpSecurity
     * @param oauth2ResourceServerProperties OAuth2 资源服务器安全配置属性
     * @param opaqueTokenIntrospector        不透明令牌 Redis 自省器
     * @param objectMapper                   JSON 操作对象
     * @return SecurityFilterChain
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   OAuth2ResourceServerProperties oauth2ResourceServerProperties,
                                                   OpaqueTokenIntrospector opaqueTokenIntrospector,
                                                   ObjectMapper objectMapper) {
        // 禁用 CSRF：资源服务器使用无状态 Bearer 令牌认证，无需 CSRF 保护
        http.csrf(AbstractHttpConfigurer::disable);

        // 无状态会话管理：不创建 Session，每次请求携带 Bearer 令牌校验
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 请求授权规则：白名单路径免认证，其余路径需要在请求头中携带有效的 Bearer 令牌
        http.authorizeHttpRequests(auth -> {
            String[] permitPaths = oauth2ResourceServerProperties.getPermitPaths().toArray(String[]::new);
            // 白名单路径免认证，直接放行
            if (permitPaths.length > 0) {
                auth.requestMatchers(permitPaths).permitAll();
            }
            // 其余路径需要认证
            auth.anyRequest().authenticated();
        });

        // OAuth2 资源服务器配置：不透明令牌本地自省 + 权限编码转换 + 统一 401 响应
        http.oauth2ResourceServer(oauth2 -> oauth2.opaqueToken(opaqueToken -> opaqueToken
                // 使用 Redis 本地自省器（直接读取授权记录，不走 /oauth2/introspect 端点）
                .introspector(opaqueTokenIntrospector)
                // 自省结果中的 authorities 字段转换为 GrantedAuthority，供 @RequiresPermission 使用
                .authenticationConverter(new DefaultOpaqueTokenAuthenticationConverter()))
            // 401 未认证返回统一 JSON 格式
            .authenticationEntryPoint(new DefaultAuthenticationEntryPoint(objectMapper)));

        // 403 权限不足返回统一 JSON 格式
        http.exceptionHandling(ex ->
            ex.accessDeniedHandler(new DefaultAccessDeniedHandler(objectMapper)));

        return http.build();
    }

    /**
     * {@link RequiresPermission} 权限校验器 Bean
     * <p>
     * 由 {@link RequiresPermissionAspect} 调用，基于当前认证主体的 {@code GrantedAuthority}
     * 集合执行权限匹配，权限不足时抛出 {@link io.github.fushuwei.scaskeleton.core.exception.ForbiddenException}。
     *
     * @return 权限校验器
     */
    @Bean(name = "requiresPermissionChecker")
    @ConditionalOnMissingBean(RequiresPermissionChecker.class)
    public RequiresPermissionChecker requiresPermissionChecker() {
        return new RequiresPermissionChecker();
    }

    /**
     * {@link RequiresPermission} 权限校验切面 Bean
     * <p>
     * 拦截标注了 {@link RequiresPermission} 的方法（或类），在方法执行前委托
     * {@link RequiresPermissionChecker} 校验权限，权限不足时抛出
     * {@link org.springframework.security.access.AccessDeniedException}，由 Spring Security
     * 统一进入 {@code AccessDeniedHandler}，返回标准 403 响应。
     *
     * @param requiresPermissionChecker 权限校验器
     * @return 权限校验切面
     */
    @Bean
    @ConditionalOnMissingBean(RequiresPermissionAspect.class)
    public RequiresPermissionAspect requiresPermissionAspect(RequiresPermissionChecker requiresPermissionChecker) {
        return new RequiresPermissionAspect(requiresPermissionChecker);
    }

    /**
     * 当前用户信息提供者，从 Bearer 不透明令牌自省结果中读取用户上下文信息
     *
     * @return 当前用户信息提供者实现
     */
    @Bean
    @ConditionalOnMissingBean(CurrentUserProvider.class)
    public CurrentUserProvider currentUserProvider() {
        return new CurrentUserProviderImpl();
    }
}
