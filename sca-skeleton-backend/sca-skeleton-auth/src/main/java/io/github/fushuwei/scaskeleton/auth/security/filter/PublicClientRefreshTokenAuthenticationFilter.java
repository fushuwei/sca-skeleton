package io.github.fushuwei.scaskeleton.auth.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 为公共客户端（{@link ClientAuthenticationMethod#NONE}）的 refresh_token grant
 * 补充客户端认证。
 * <p>
 * Spring Authorization Server 7.1.x 的 {@code PublicClientAuthenticationConverter}
 * 仅匹配 PKCE token 请求（{@code grant_type=authorization_code} + {@code code_verifier}），
 * 对 {@code grant_type=refresh_token} 返回 {@code null}，导致公共客户端续期时
 * 客户端认证失败 → token 端点返回 401。
 * <p>
 * 本过滤器在 SAS 默认认证之前执行：当请求为 refresh_token grant 且包含
 * {@code client_id} 参数时，直接构造公共客户端认证令牌写入 SecurityContext，
 * 让后续的 {@code OAuth2RefreshTokenAuthenticationProvider} 能正确获取客户端身份。
 *
 * @author Fu Wei
 */
public class PublicClientRefreshTokenAuthenticationFilter extends OncePerRequestFilter {

    private final RegisteredClientRepository registeredClientRepository;

    public PublicClientRefreshTokenAuthenticationFilter(RegisteredClientRepository registeredClientRepository) {
        this.registeredClientRepository = registeredClientRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // 仅处理 token 端点的 refresh_token grant
        if (!isRefreshTokenRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientId = request.getParameter(OAuth2ParameterNames.CLIENT_ID);
        if (!StringUtils.hasText(clientId)) {
            filterChain.doFilter(request, response);
            return;
        }

        RegisteredClient registeredClient = this.registeredClientRepository.findByClientId(clientId);
        if (registeredClient == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 仅处理公共客户端（NONE 认证方式），confidential 客户端由其他 converter 处理
        if (!registeredClient.getClientAuthenticationMethods()
                .contains(ClientAuthenticationMethod.NONE)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 构建公共客户端认证令牌，写入 SecurityContext
        OAuth2ClientAuthenticationToken clientAuthentication =
                new OAuth2ClientAuthenticationToken(
                        registeredClient, ClientAuthenticationMethod.NONE, null);
        SecurityContextHolder.getContext().setAuthentication(clientAuthentication);

        filterChain.doFilter(request, response);
    }

    /** 判断是否为 token 端点的 refresh_token grant 请求 */
    private boolean isRefreshTokenRequest(HttpServletRequest request) {
        // 使用 endsWith 兼容网关 StripPrefix 等不同部署拓扑
        // filter 本身在 SAS 安全链内，安全链已限定只处理 /oauth2/** 路径
        String uri = request.getRequestURI();
        return (uri.endsWith("/oauth2/token"))
                && "POST".equalsIgnoreCase(request.getMethod())
                && "refresh_token".equals(request.getParameter(OAuth2ParameterNames.GRANT_TYPE));
    }
}
