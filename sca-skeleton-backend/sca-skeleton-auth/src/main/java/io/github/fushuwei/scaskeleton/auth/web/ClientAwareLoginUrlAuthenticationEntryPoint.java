package io.github.fushuwei.scaskeleton.auth.web;

import io.github.fushuwei.scaskeleton.auth.config.properties.OAuthClientsProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.io.IOException;

/**
 * 客户端感知的登录入口：根据 OAuth2 authorize 请求中的 {@code client_id} 跳转到不同登录页。
 * <p>
 * admin 客户端 → 网关 {@code /auth/login/admin}；portal 客户端 → 网关 {@code /auth/login/portal}。
 *
 * @author Fu Wei
 */
public class ClientAwareLoginUrlAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {

    /** OAuth2 客户端配置，用于 client_id → 登录页 URL 映射 */
    private final OAuthClientsProperties oauthClientsProperties;

    /**
     * @param oauthClientsProperties 客户端配置（admin / portal）
     * @param defaultLoginUrl        未知 client_id 时的默认登录页绝对 URL
     */
    public ClientAwareLoginUrlAuthenticationEntryPoint(OAuthClientsProperties oauthClientsProperties,
            String defaultLoginUrl) {
        super(defaultLoginUrl);
        this.oauthClientsProperties = oauthClientsProperties;
    }

    /**
     * 未登录访问授权端点时，直接 302 到经网关暴露的绝对登录 URL。
     * <p>
     * 覆盖父类 {@code commence}，避免父类按 Auth 内网地址重写 Location。
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        // 从 authorize 请求 query 读取 client_id，映射 admin / portal 登录页
        String clientId = request.getParameter("client_id");
        String loginUrl = oauthClientsProperties.resolveExternalLoginUrl(clientId);
        // 302 到网关登录页（浏览器后续请求仍走网关 /auth/**）
        response.sendRedirect(loginUrl);
    }
}
