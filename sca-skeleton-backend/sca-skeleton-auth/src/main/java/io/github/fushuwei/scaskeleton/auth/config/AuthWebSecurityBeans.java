package io.github.fushuwei.scaskeleton.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

/**
 * Auth Web 安全相关共享 Bean（供多条 SecurityFilterChain 复用）。
 *
 * @author Fu Wei
 */
@Configuration(proxyBeanMethods = false)
public class AuthWebSecurityBeans {

    /**
     * 跨 Order(1) SAS 链与 Order(2) 表单登录链共享的 SavedRequest 缓存。
     * <p>
     * 仅缓存 {@code /oauth2/**}，避免登录页 GET 覆盖 authorize SavedRequest。
     */
    @Bean
    public HttpSessionRequestCache httpSessionRequestCache() {
        HttpSessionRequestCache cache = new HttpSessionRequestCache();
        cache.setRequestMatcher(request -> {
            String uri = request.getRequestURI();
            return uri != null && uri.startsWith("/oauth2/");
        });
        return cache;
    }
}
