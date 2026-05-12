package io.github.fushuwei.sca.starter.security.config;

import tools.jackson.databind.ObjectMapper;
import io.github.fushuwei.sca.starter.core.user.CurrentUserProvider;
import io.github.fushuwei.sca.starter.security.handler.SecurityAccessDeniedHandler;
import io.github.fushuwei.sca.starter.security.handler.SecurityAuthenticationEntryPoint;
import io.github.fushuwei.sca.starter.security.introspection.PermissionsOpaqueTokenAuthenticationConverter;
import io.github.fushuwei.sca.starter.security.properties.SecurityProperties;
import io.github.fushuwei.sca.starter.security.user.CurrentUserProviderImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

/**
 * Security Resource Server 自动配置入口。
 * <p>
 * 配置内容：
 * <ol>
 *   <li>无状态 Session（Bearer 不透明令牌 + 自省，无 Cookie/Session）</li>
 *   <li>禁用 CSRF（Bearer Token 场景无需 CSRF 防护）</li>
 *   <li>白名单路径免认证，其余路径均需通过 introspection 校验的 access_token</li>
 *   <li>自省端点与客户端凭证取自 {@code spring.security.oauth2.resourceserver.opaquetoken.*}</li>
 *   <li>权限映射：{@link PermissionsOpaqueTokenAuthenticationConverter} 从 {@code permissions} 声明生成 GrantedAuthority</li>
 *   <li>统一 401/403 JSON 响应</li>
 *   <li>开启方法级权限注解：{@code @PreAuthorize}、{@code @PostAuthorize}、{@code @Secured}</li>
 * </ol>
 *
 * @author Fu Wei
 */
@AutoConfiguration
@EnableMethodSecurity(securedEnabled = true)
@EnableConfigurationProperties({SecurityProperties.class, OAuth2ResourceServerProperties.class})
public class ResourceServerAutoConfiguration {

    /**
     * 注册资源服务 SecurityFilterChain：不透明令牌自省、白名单、异常响应。
     *
     * @param http                        HttpSecurity
     * @param securityProperties          sca.security.*（白名单、claims 字段名）
     * @param oauth2ResourceServerProperties Boot 标准 opaquetoken 配置
     * @param objectMapper                JSON 异常响应
     * @return SecurityFilterChain
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain resourceServerSecurityFilterChain(
            HttpSecurity http,
            SecurityProperties securityProperties,
            OAuth2ResourceServerProperties oauth2ResourceServerProperties,
            ObjectMapper objectMapper) throws Exception {

        OAuth2ResourceServerProperties.Opaquetoken opaque = oauth2ResourceServerProperties.getOpaquetoken();
        if (!StringUtils.hasText(opaque.getIntrospectionUri())
                || !StringUtils.hasText(opaque.getClientId())
                || !StringUtils.hasText(opaque.getClientSecret())) {
            throw new IllegalStateException(
                    "不透明资源服务器需配置 spring.security.oauth2.resourceserver.opaquetoken "
                            + "(introspection-uri, client-id, client-secret)");
        }
        OpaqueTokenIntrospector introspector = new SpringOpaqueTokenIntrospector(
                opaque.getIntrospectionUri(), opaque.getClientId(), opaque.getClientSecret());

        http.csrf(AbstractHttpConfigurer::disable);
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> {
            String[] permitPaths = securityProperties.getPermitPaths().toArray(String[]::new);
            if (permitPaths.length > 0) {
                auth.requestMatchers(permitPaths).permitAll();
            }
            auth.anyRequest().authenticated();
        });

        http.oauth2ResourceServer(oauth2 -> oauth2
                .opaqueToken(opaqueToken -> opaqueToken
                        .introspector(introspector)
                        .authenticationConverter(new PermissionsOpaqueTokenAuthenticationConverter()))
                .authenticationEntryPoint(new SecurityAuthenticationEntryPoint(objectMapper)));

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
        return new CurrentUserProviderImpl(securityProperties);
    }
}
