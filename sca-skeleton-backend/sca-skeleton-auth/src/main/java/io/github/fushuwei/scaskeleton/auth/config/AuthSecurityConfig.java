package io.github.fushuwei.scaskeleton.auth.config;

import io.github.fushuwei.scaskeleton.auth.security.RoutingUserDetailsService;
import io.github.fushuwei.scaskeleton.auth.security.filter.LoginChannelFilter;
import io.github.fushuwei.scaskeleton.auth.web.ChannelAwareAuthenticationFailureHandler;
import io.github.fushuwei.scaskeleton.auth.web.LoginPageController;
import io.github.fushuwei.scaskeleton.auth.web.OAuthAuthorizeLoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Auth 服务默认安全过滤链：托管 admin / portal 登录页与表单认证（Order=2，低于 SAS 端点链）。
 *
 * @author Fu Wei
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@RequiredArgsConstructor
public class AuthSecurityConfig {

    /** 登录渠道解析过滤器（admin / portal） */
    private final LoginChannelFilter loginChannelFilter;

    /** 登录失败回跳处理器 */
    private final ChannelAwareAuthenticationFailureHandler authenticationFailureHandler;

    /** 登录成功后恢复 OAuth2 authorize SavedRequest */
    private final OAuthAuthorizeLoginSuccessHandler oauthAuthorizeLoginSuccessHandler;

    /** 与 SAS 过滤链共享的 SavedRequest 缓存 */
    private final HttpSessionRequestCache httpSessionRequestCache;

    /**
     * 默认安全过滤链：登录页、表单认证、Actuator 健康检查。
     *
     * @param http                    HttpSecurity
     * @param routingUserDetailsService 按渠道路由的用户加载服务
     * @return Order=2 的 FilterChain
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            RoutingUserDetailsService routingUserDetailsService) throws Exception {

        http
            // 不处理 OAuth2 / OIDC 端点，避免与 Order=1 的 SAS 链争抢匹配
            .securityMatcher(oauthEndpointsExcludedMatcher())
            // 仅放行统一登出接口的 CSRF（前端通过表单 POST 携带 token，避免 token 暴露到 URL）
            .csrf(csrf -> csrf.ignoringRequestMatchers("/logout"))
            // 表单登录启用 CSRF；OAuth2 标准端点由 Order=1 的 SAS 过滤链单独处理
            .authorizeHttpRequests(authorize -> authorize
                // 健康检查无需认证
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // admin / portal 登录页 GET 与表单 POST 放行
                .requestMatchers("/login/**").permitAll()
                // 其余请求需 Session 认证（authorize 链路登录成功后持有 Session）
                .anyRequest().authenticated()
            )
            // 在 UsernamePasswordAuthenticationFilter 之前解析 loginChannel
            .addFilterBefore(loginChannelFilter, UsernamePasswordAuthenticationFilter.class)
            .userDetailsService(routingUserDetailsService)
            .requestCache(cache -> cache.requestCache(httpSessionRequestCache))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .formLogin(form -> form
                // 默认 loginPage（实际入口由 ClientAwareLoginUrlAuthenticationEntryPoint 按 client 分流）
                .loginPage("/login/admin")
                // 统一表单处理 URL（admin / portal 表单均 POST 到此）
                .loginProcessingUrl(LoginPageController.LOGIN_PROCESSING_URL)
                .successHandler(oauthAuthorizeLoginSuccessHandler)
                .failureHandler(authenticationFailureHandler)
                .permitAll()
            );

        return http.build();
    }

    /** 排除 SAS 端点，仅供 Order(2) 表单登录链使用。 */
    private RequestMatcher oauthEndpointsExcludedMatcher() {
        return request -> {
            String uri = request.getRequestURI();
            return !uri.startsWith("/oauth2/") && !uri.startsWith("/.well-known/");
        };
    }

    /** 密码编码器：使用 Spring Security 默认委托实现。 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
