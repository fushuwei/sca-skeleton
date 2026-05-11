package io.github.fushuwei.sca.starter.security.config;

import tools.jackson.databind.ObjectMapper;
import io.github.fushuwei.sca.starter.core.user.CurrentUserProvider;
import io.github.fushuwei.sca.starter.security.handler.SecurityAccessDeniedHandler;
import io.github.fushuwei.sca.starter.security.handler.SecurityAuthenticationEntryPoint;
import io.github.fushuwei.sca.starter.security.properties.SecurityProperties;
import io.github.fushuwei.sca.starter.security.user.CurrentUserProviderImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Resource Server 自动配置入口。
 * <p>
 * 配置内容：
 * <ol>
 *   <li>无状态 Session（纯 JWT，无 Cookie/Session）</li>
 *   <li>禁用 CSRF（JWT Bearer Token 无需 CSRF 防护）</li>
 *   <li>白名单路径免认证，其余路径均需有效 JWT</li>
 *   <li>JWT 解码器：优先使用 jwkSetUri，否则通过 issuerUri 自动推导</li>
 *   <li>JWT Authorities 转换：将 {@code scope} 字段映射为 {@code SCOPE_} 前缀的权限</li>
 *   <li>统一 401/403 JSON 响应，替换 Spring Security 默认的 HTML 页面</li>
 *   <li>开启方法级权限注解：{@code @PreAuthorize}、{@code @PostAuthorize}、{@code @Secured}</li>
 * </ol>
 * <p>
 * 使用 ConditionalOnMissingBean(SecurityFilterChain.class) 确保业务模块可完全自定义安全配置。
 *
 * @author Fu Wei
 */
@AutoConfiguration
@EnableMethodSecurity(securedEnabled = true)
@EnableConfigurationProperties(SecurityProperties.class)
public class ResourceServerAutoConfiguration {

    /**
     * 注册资源服务 SecurityFilterChain，处理 JWT 认证、白名单路径、异常响应等。
     *
     * @param http               HttpSecurity 构建器
     * @param securityProperties 安全配置属性（白名单路径、JWT Claims 字段名等）
     * @param objectMapper       Jackson 序列化器，用于 JSON 格式异常响应
     * @return 资源服务安全过滤器链
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain resourceServerSecurityFilterChain(
            HttpSecurity http,
            SecurityProperties securityProperties,
            ObjectMapper objectMapper) throws Exception {

        // 禁用 CSRF：纯 JWT Bearer Token 认证无需 CSRF 防护
        http.csrf(AbstractHttpConfigurer::disable);

        // 无状态 Session：服务端不保存任何 Session，每次请求携带 JWT 完成认证
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 路径授权：白名单路径免认证，其余全部要求有效 JWT
        http.authorizeHttpRequests(auth -> {
            // 从配置属性中读取白名单路径并设置为允许匿名访问
            String[] permitPaths = securityProperties.getPermitPaths().toArray(String[]::new);
            if (permitPaths.length > 0) {
                auth.requestMatchers(permitPaths).permitAll();
            }
            // 其余所有请求均需通过认证
            auth.anyRequest().authenticated();
        });

        // 配置 JWT 资源服务器
        http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter(securityProperties)))
                // 未认证（Token 缺失或无效）时返回统一 JSON 格式的 401 响应
                .authenticationEntryPoint(new SecurityAuthenticationEntryPoint(objectMapper))
        );

        // 权限不足（已认证但无权访问）时返回统一 JSON 格式的 403 响应
        http.exceptionHandling(ex ->
                ex.accessDeniedHandler(new SecurityAccessDeniedHandler(objectMapper)));

        return http.build();
    }

    /**
     * 注册 CurrentUserProvider 实现 Bean，从 JWT Claims 中提取当前用户信息。
     * 供 MyBatis-Plus 审计字段填充、操作日志等通用能力使用。
     *
     * @param securityProperties 安全配置属性
     * @return CurrentUserProviderImpl 实例
     */
    @Bean
    @ConditionalOnMissingBean(CurrentUserProvider.class)
    public CurrentUserProvider currentUserProvider(SecurityProperties securityProperties) {
        return new CurrentUserProviderImpl(securityProperties);
    }

    /**
     * 配置 JWT Authorities 转换器。
     * 将 JWT Claims 中的 {@code scope} 字段转换为 Spring Security 权限，
     * 前缀为 {@code SCOPE_}（如 scope=read 映射为 SCOPE_read）。
     * 如需自定义权限字段（如 roles），可在业务模块中覆盖此 Bean。
     *
     * @param securityProperties 安全配置属性
     * @return JWT 认证转换器
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter(SecurityProperties securityProperties) {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // scope 字段转换为 SCOPE_ 前缀的权限，兼容 Spring Authorization Server 标准
        authoritiesConverter.setAuthoritiesClaimName("scope");
        authoritiesConverter.setAuthorityPrefix("SCOPE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        // 将 Claims 中的用户 ID 字段作为 principal name
        converter.setPrincipalClaimName(securityProperties.getUserIdClaimName());
        return converter;
    }
}
