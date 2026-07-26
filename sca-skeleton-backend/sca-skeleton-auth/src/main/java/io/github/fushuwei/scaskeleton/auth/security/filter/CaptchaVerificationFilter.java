package io.github.fushuwei.scaskeleton.auth.security.filter;

import io.github.fushuwei.scaskeleton.auth.config.properties.OAuth2ClientProperties;
import io.github.fushuwei.scaskeleton.captcha.CaptchaService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 验证码校验过滤器：在密码模式令牌请求认证前验证图形验证码。
 * <p>
 * 仅拦截 {@code POST /oauth2/token} 且 {@code grant_type=password} 且当前客户端为 portal 渠道时生效；
 * admin 渠道不要求验证码（管理后台运维场景，简化登录流程）。
 * <p>
 * 客户端身份从 {@link SecurityContextHolder} 中的 {@link OAuth2ClientAuthenticationToken} 获取
 * （由 SAS 的 OAuth2ClientAuthenticationFilter 在本过滤器之前完成认证并写入 SecurityContext），
 * 而非从请求参数中读取 client_id（机密客户端使用 {@code client_secret_basic} 时 client_id 在 Authorization 头中）。
 * <p>
 * 校验时读取 {@code captcha_key} 与 {@code captcha_code} 参数，
 * 调用 {@link CaptchaService#verify} 进行校验。
 * 校验失败时返回标准 OAuth2 JSON 错误响应（不重定向，适配 SPA AJAX 调用）。
 *
 * @author Fu Wei
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CaptchaVerificationFilter extends OncePerRequestFilter {

    /** SAS token 端点 URI */
    private static final String TOKEN_URI = "/oauth2/token";

    /** 请求参数名：grant_type */
    private static final String PARAM_GRANT_TYPE = "grant_type";

    /** 请求参数名：验证码唯一标识 */
    private static final String PARAM_CAPTCHA_KEY = "captcha_key";

    /** 请求参数名：用户输入的验证码文本 */
    private static final String PARAM_CAPTCHA_CODE = "captcha_code";

    /** JSON 序列化器（Spring 容器中已存在的 ObjectMapper Bean） */
    private final ObjectMapper objectMapper;

    private final CaptchaService captchaService;

    private final OAuth2ClientProperties oauth2ClientProperties;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        // 仅处理 token 端点 POST 请求
        if (!isTokenEndpointPost(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 仅处理密码模式请求
        String grantType = request.getParameter(PARAM_GRANT_TYPE);
        if (!"password".equals(grantType)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 从 SecurityContext 获取已认证的客户端（由 OAuth2ClientAuthenticationFilter 在本过滤器之前完成认证）
        RegisteredClient registeredClient = getRegisteredClient();
        if (registeredClient == null) {
            // 客户端未认证（SAS 会后续处理并返回 invalid_client），跳过验证码校验
            filterChain.doFilter(request, response);
            return;
        }

        // 仅 portal 渠道强制校验图形验证码
        if (!isPortalClient(registeredClient)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientId = registeredClient.getClientId();

        // 读取验证码参数
        String captchaKey = request.getParameter(PARAM_CAPTCHA_KEY);
        String captchaCode = request.getParameter(PARAM_CAPTCHA_CODE);

        // portal 渠道未提交验证码时返回错误
        if (!StringUtils.hasText(captchaKey) || !StringUtils.hasText(captchaCode)) {
            log.warn("密码模式验证码校验失败：client_id={}, 原因=验证码参数缺失", clientId);
            writeOAuth2Error(response, "invalid_request", "验证码不能为空");
            return;
        }

        // 调用验证码服务校验（内部校验后立即删除 Redis key，一次性使用）
        boolean verified = captchaService.verify(captchaKey, captchaCode);
        if (!verified) {
            log.warn("密码模式验证码校验失败：client_id={}, captcha_key={}", clientId, captchaKey);
            writeOAuth2Error(response, "invalid_request", "验证码错误，请重新输入");
            return;
        }

        // 验证通过，继续后续认证流程
        filterChain.doFilter(request, response);
    }

    /**
     * 判断是否为 token 端点 POST 请求。
     * <p>
     * 使用 {@code getServletPath()} 而非 {@code getRequestURI()}，
     * 以在配置了 context-path 的部署环境中正确匹配。
     */
    private boolean isTokenEndpointPost(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && TOKEN_URI.equals(request.getServletPath());
    }

    /**
     * 从 SecurityContext 获取已认证的客户端的 RegisteredClient。
     * <p>
     * SAS 的 OAuth2ClientAuthenticationFilter 在本过滤器之前执行，
     * 已将 OAuth2ClientAuthenticationToken 写入 SecurityContext。
     */
    private RegisteredClient getRegisteredClient() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OAuth2ClientAuthenticationToken clientToken
                && clientToken.isAuthenticated()) {
            return clientToken.getRegisteredClient();
        }
        return null;
    }

    /**
     * 判断已认证的客户端是否为 portal 客户端。
     */
    private boolean isPortalClient(RegisteredClient registeredClient) {
        return oauth2ClientProperties.getPortal().getClientId() != null
                && oauth2ClientProperties.getPortal().getClientId().equals(registeredClient.getClientId());
    }

    /**
     * 返回标准 OAuth2 JSON 错误响应（使用 Jackson 序列化，确保 JSON 转义正确）。
     */
    private void writeOAuth2Error(HttpServletResponse response, String error, String errorDescription) throws IOException {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("error_description", errorDescription);
        body.put("timestamp", System.currentTimeMillis());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
