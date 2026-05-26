package io.github.fushuwei.scaskeleton.auth.security.filter;

import io.github.fushuwei.scaskeleton.auth.security.LoginChannel;
import io.github.fushuwei.scaskeleton.auth.security.LoginChannelContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 登录渠道过滤器：在用户名密码认证前解析 {@code loginChannel} 表单字段。
 * <p>
 * 仅拦截 {@code POST /login/authenticate}，将 admin / portal 写入 {@link LoginChannelContext}，
 * 供 {@link io.github.fushuwei.scaskeleton.auth.security.RoutingUserDetailsService} 选择用户加载策略。
 *
 * @author Fu Wei
 */
@Component
public class LoginChannelFilter extends OncePerRequestFilter {

    /** 统一登录表单处理 URL，与 {@code AuthSecurityConfig#formLogin} 保持一致 */
    private static final String LOGIN_PROCESSING_URI = "/login/authenticate";

    /** 表单字段名：标识 admin 或 portal 登录页来源 */
    private static final String PARAM_LOGIN_CHANNEL = "loginChannel";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        // 仅处理登录表单 POST，其它请求直接放行
        if (isLoginAuthenticatePost(request)) {
            // 从表单读取渠道并绑定到当前线程
            String rawChannel = request.getParameter(PARAM_LOGIN_CHANNEL);
            LoginChannelContext.set(LoginChannel.fromValue(rawChannel));
        }
        try {
            // 继续后续 Security 过滤器链（含 UsernamePasswordAuthenticationFilter）
            filterChain.doFilter(request, response);
        } finally {
            // 请求结束必须清理 ThreadLocal，避免线程池复用导致渠道串扰
            LoginChannelContext.clear();
        }
    }

    /**
     * 判断是否为登录表单提交请求。
     *
     * @param request 当前 HTTP 请求
     * @return true 表示 POST /login/authenticate
     */
    private boolean isLoginAuthenticatePost(HttpServletRequest request) {
        // 方法必须为 POST
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        // URI 精确匹配登录处理端点（网关 StripPrefix 后 auth 服务看到的路径）
        return LOGIN_PROCESSING_URI.equals(request.getRequestURI());
    }
}
