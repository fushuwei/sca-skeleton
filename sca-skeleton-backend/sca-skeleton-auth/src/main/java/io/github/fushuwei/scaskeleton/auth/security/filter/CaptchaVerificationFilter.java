package io.github.fushuwei.scaskeleton.auth.security.filter;

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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 验证码校验过滤器：在密码模式令牌请求认证前验证图形验证码。
 * <p>
 * 拦截 {@code POST /oauth2/token} 且 {@code grant_type=password} 的请求，对所有渠道
 * （admin / portal）均强制校验图形验证码。
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

    /**
     * JSON 序列化器（注入容器中的全局 {@link JsonMapper} Bean）。
     * <p>
     * 由 {@code sca-skeleton-starter-core} 的 {@code JacksonAutoConfiguration} 注册，
     * 已配置统一的时区、JavaTimeModule 等序列化策略。
     */
    private final JsonMapper jsonMapper;

    private final CaptchaService captchaService;

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

        // 读取验证码参数
        String captchaKey = request.getParameter(PARAM_CAPTCHA_KEY);
        String captchaCode = request.getParameter(PARAM_CAPTCHA_CODE);

        // 未提交验证码时返回错误
        if (!StringUtils.hasText(captchaKey) || !StringUtils.hasText(captchaCode)) {
            log.warn("密码模式验证码校验失败：验证码参数缺失");
            writeOAuth2Error(response, "invalid_request", "验证码不能为空");
            return;
        }

        // 调用验证码服务校验（内部校验后立即删除 Redis key，一次性使用）
        boolean verified = captchaService.verify(captchaKey, captchaCode);
        if (!verified) {
            log.warn("密码模式验证码校验失败：captcha_key={}", captchaKey);
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
     * 返回标准 OAuth2 JSON 错误响应（使用 Jackson 序列化，确保 JSON 转义正确）。
     * <p>
     * 必须显式设置 UTF-8 字符编码，否则 {@code response.getWriter()} 会使用默认编码
     * （ISO-8859-1），导致中文 error_description 变成乱码（一堆 ?）。
     */
    private void writeOAuth2Error(HttpServletResponse response, String error, String errorDescription) throws IOException {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("error_description", errorDescription);
        body.put("timestamp", System.currentTimeMillis());
        response.getWriter().write(jsonMapper.writeValueAsString(body));
    }
}
