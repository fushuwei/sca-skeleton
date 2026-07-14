package io.github.fushuwei.scaskeleton.auth.config;

import io.github.fushuwei.scaskeleton.auth.config.properties.OAuth2ClientProperties;
import io.github.fushuwei.scaskeleton.auth.security.RoutingUserDetailsService;
import io.github.fushuwei.scaskeleton.auth.security.filter.CaptchaVerificationFilter;
import io.github.fushuwei.scaskeleton.auth.security.filter.LoginChannelFilter;
import io.github.fushuwei.scaskeleton.auth.security.handler.ChannelAwareAuthenticationFailureHandler;
import io.github.fushuwei.scaskeleton.auth.web.LoginPageController;
import io.github.fushuwei.scaskeleton.auth.security.handler.OAuth2AuthorizeLoginSuccessHandler;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

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

    /** 验证码校验过滤器（在认证前验证图形验证码） */
    private final CaptchaVerificationFilter captchaVerificationFilter;

    /** 登录失败回跳处理器 */
    private final ChannelAwareAuthenticationFailureHandler authenticationFailureHandler;

    /** 登录成功后恢复 OAuth2 authorize SavedRequest */
    private final OAuth2AuthorizeLoginSuccessHandler oauthAuthorizeLoginSuccessHandler;

    /** 与 SAS 过滤链共享的 SavedRequest 缓存 */
    private final HttpSessionRequestCache httpSessionRequestCache;

    /** OAuth 客户端配置（用于退出后按渠道重定向） */
    private final OAuth2ClientProperties oauth2ClientProperties;

    /** OAuth2 授权存储服务（用于退出时吊销令牌） */
    private final OAuth2AuthorizationService oAuth2AuthorizationService;

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
            // 不处理 OAuth2 端点，避免与 Order=1 的 SAS 链争抢匹配
            .securityMatcher(oauthEndpointsExcludedMatcher())
            // CSRF 保护配置：
            // - /logout: 前端通过表单 POST 携带 token，避免 token 暴露到 URL
            // - /login/authenticate: 登录表单提交，OAuth2 PKCE 已通过 state 参数提供 CSRF 保护
            //   且 admin/portal 可能共享 session，需要禁用 CSRF 避免跨标签页登录冲突
            .csrf(csrf -> csrf.ignoringRequestMatchers("/logout", "/login/authenticate"))
            // 表单登录启用 CSRF；OAuth2 标准端点由 Order=1 的 SAS 过滤链单独处理
            .authorizeHttpRequests(authorize -> authorize
                // 健康检查无需认证
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // 统一退出端点：LogoutFilter 已配置 permitAll，此处作为 AuthorizationFilter 的兜底放行
                .requestMatchers("/logout").permitAll()
                // admin / portal 登录页 GET 与表单 POST 放行
                .requestMatchers("/login/**").permitAll()
                // 验证码生成接口放行（同时兼容网关 StripPrefix 后的路径和直连路径）
                .requestMatchers("/captcha/**", "/auth/captcha/**").permitAll()
                // 静态资源放行（同时兼容网关 StripPrefix 后的路径和直连路径）
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                // API 文档放行（SpringDoc OpenAPI、Scalar UI 及 WebJars 静态资源）
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                        "/scalar/**", "/webjars/**").permitAll()
                // 其余请求需 Session 认证（authorize 链路登录成功后持有 Session）
                .anyRequest().authenticated()
            )
            // 在 UsernamePasswordAuthenticationFilter 之前：① 解析 loginChannel → ② 校验验证码 → ③ 认证
            .addFilterBefore(loginChannelFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(captchaVerificationFilter, UsernamePasswordAuthenticationFilter.class)
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
            )
            // 统一退出配置：LogoutFilter 在 AuthorizationFilter 之前拦截 POST /logout，
            // 完成令牌吊销 → Session 销毁 → 按渠道重定向到经网关的正确登录页
            .logout(logout -> logout
                .logoutUrl("/logout")
                .permitAll()   // 允许未认证用户执行退出（前端通过表单参数提交 token）
                .addLogoutHandler(this::revokeTokens)
                .logoutSuccessHandler(this::onLogoutSuccess)
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

    /**
     * 退出 LogoutHandler：根据表单参数吊销 access_token / refresh_token。
     */
    private void revokeTokens(jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            org.springframework.security.core.Authentication authentication) {
        String accessToken = request.getParameter("access_token");
        String refreshToken = request.getParameter("refresh_token");
        // 防止 access / refresh 指向同一 authorization 时重复删除
        Set<String> removedIds = new HashSet<>();
        revokeTokenIfPresent(accessToken, OAuth2TokenType.ACCESS_TOKEN, removedIds);
        revokeTokenIfPresent(refreshToken, OAuth2TokenType.REFRESH_TOKEN, removedIds);
    }

    private void revokeTokenIfPresent(String tokenValue, OAuth2TokenType tokenType, Set<String> removedIds) {
        if (!StringUtils.hasText(tokenValue)) {
            return;
        }
        OAuth2Authorization authorization = oAuth2AuthorizationService.findByToken(tokenValue, tokenType);
        if (authorization == null || !removedIds.add(authorization.getId())) {
            return;
        }
        oAuth2AuthorizationService.remove(authorization);
    }

    /**
     * 退出 LogoutSuccessHandler：销毁 Session → 清理 SecurityContext → 重定向到对应登录页。
     */
    private void onLogoutSuccess(jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            org.springframework.security.core.Authentication authentication) throws java.io.IOException {
        // 销毁服务端 Session（含 pending authorize 与登录渠道信息）
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        // 清理线程上下文
        SecurityContextHolder.clearContext();
        // 按渠道回到对应登录页
        String channel = request.getParameter("channel");
        String clientId = resolveClientId(channel);
        response.sendRedirect(oauth2ClientProperties.resolveExternalLoginUrl(clientId));
    }

    /** 将 channel 参数映射为对应 clientId，未知值默认 admin。 */
    private String resolveClientId(String channel) {
        if (StringUtils.hasText(channel) && "portal".equalsIgnoreCase(channel)) {
            return oauth2ClientProperties.getPortal().getClientId();
        }
        return oauth2ClientProperties.getAdmin().getClientId();
    }

}
