package io.github.fushuwei.scaskeleton.auth.security.filter;

import io.github.fushuwei.scaskeleton.auth.config.properties.OAuthClientsProperties;
import io.github.fushuwei.scaskeleton.auth.security.LoginChannel;
import io.github.fushuwei.scaskeleton.auth.captcha.CaptchaService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 验证码校验过滤器：在表单认证前验证图形验证码。
 * <p>
 * 仅拦截 {@code POST /login/authenticate}，读取 {@code captchaKey} 与
 * {@code captchaCode} 表单字段，调用 {@link CaptchaService#verify} 进行校验。
 * 校验失败时根据 {@code loginChannel} 参数回跳对应登录页并携带
 * {@code captcha-error} 标记；校验通过后继续过滤器链。
 * <p>
 * 需注册在 {@link LoginChannelFilter} 之后、{@code UsernamePasswordAuthenticationFilter} 之前。
 *
 * @author Fu Wei
 */
@Component
@RequiredArgsConstructor
public class CaptchaVerificationFilter extends OncePerRequestFilter {

    /** 统一登录表单处理 URL */
    private static final String LOGIN_PROCESSING_URI = "/login/authenticate";

    /** 表单字段名：验证码唯一标识 */
    private static final String PARAM_CAPTCHA_KEY = "captchaKey";

    /** 表单字段名：用户输入的验证码文本 */
    private static final String PARAM_CAPTCHA_CODE = "captchaCode";

    /** 表单字段名：登录渠道（admin / portal） */
    private static final String PARAM_LOGIN_CHANNEL = "loginChannel";

    /** 验证码校验失败时的查询参数名 */
    private static final String CAPTCHA_ERROR_PARAM = "captcha-error";

    private final CaptchaService captchaService;

    private final OAuthClientsProperties oauthClientsProperties;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        // 仅处理登录表单 POST
        if (!isLoginAuthenticatePost(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 读取表单中的验证码字段
        String captchaKey = request.getParameter(PARAM_CAPTCHA_KEY);
        String captchaCode = request.getParameter(PARAM_CAPTCHA_CODE);

        // 验证码为空时直接放行（前端已做非空校验；后端再次校验避免绕过）
        if (!StringUtils.hasText(captchaKey) || !StringUtils.hasText(captchaCode)) {
            redirectWithCaptchaError(request, response);
            return;
        }

        // 调用验证码服务校验（内部校验后立即删除 Redis key，一次性使用）
        boolean verified = captchaService.verify(captchaKey, captchaCode);
        if (!verified) {
            redirectWithCaptchaError(request, response);
            return;
        }

        // 验证通过，继续后续认证流程
        filterChain.doFilter(request, response);
    }

    /**
     * 判断是否为登录表单提交请求。
     */
    private boolean isLoginAuthenticatePost(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && LOGIN_PROCESSING_URI.equals(request.getRequestURI());
    }

    /**
     * 验证码校验失败时，302 重定向回登录页并携带 {@code captcha-error} 标记。
     */
    private void redirectWithCaptchaError(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String loginChannel = request.getParameter(PARAM_LOGIN_CHANNEL);
        String clientId = resolveClientId(loginChannel);
        String failureUrl = oauthClientsProperties.resolveExternalLoginFailureUrl(clientId)
                + "&" + CAPTCHA_ERROR_PARAM;
        response.sendRedirect(failureUrl);
    }

    /**
     * 将 loginChannel 映射为 clientId，用于构造回跳 URL。
     */
    private String resolveClientId(String loginChannel) {
        if (LoginChannel.PORTAL.getValue().equals(loginChannel)) {
            return oauthClientsProperties.getPortal().getClientId();
        }
        return oauthClientsProperties.getAdmin().getClientId();
    }
}
