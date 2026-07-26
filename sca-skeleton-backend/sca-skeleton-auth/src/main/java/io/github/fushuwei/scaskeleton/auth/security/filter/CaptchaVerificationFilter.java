package io.github.fushuwei.scaskeleton.auth.security.filter;

import io.github.fushuwei.scaskeleton.auth.grant.base.OAuth2ResourceOwnerBaseAuthenticationProvider;
import io.github.fushuwei.scaskeleton.auth.security.LoginLogPublisher;
import io.github.fushuwei.scaskeleton.captcha.CaptchaService;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.core.result.ResultType;
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

/**
 * 验证码校验过滤器：在密码模式令牌请求认证前验证图形验证码。
 * <p>
 * 拦截 {@code POST /oauth2/token} 且 {@code grant_type=password} 的请求，对所有渠道
 * （admin / portal）均强制校验图形验证码。
 * <p>
 * 校验失败时返回统一的 {@link Result} 格式响应（HTTP 200），同时通过 {@link LoginLogPublisher}
 * 记录登录失败日志，确保验证码错误也留有审计痕迹。
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

    /** 请求参数名：username */
    private static final String PARAM_USERNAME = "username";

    /** 请求参数名：验证码唯一标识 */
    private static final String PARAM_CAPTCHA_KEY = "captcha_key";

    /** 请求参数名：用户输入的验证码文本 */
    private static final String PARAM_CAPTCHA_CODE = "captcha_code";

    private final JsonMapper jsonMapper;

    private final CaptchaService captchaService;

    /** 登录日志发布器：验证码校验失败时记录登录失败日志 */
    private final LoginLogPublisher loginLogPublisher;

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

        // 记录登录开始时间，供 LoginLogPublisher 计算 costMs（与认证 Provider 一致）
        request.setAttribute(OAuth2ResourceOwnerBaseAuthenticationProvider.ATTR_LOGIN_START_TIME,
            System.currentTimeMillis());

        // 读取验证码参数
        String captchaKey = request.getParameter(PARAM_CAPTCHA_KEY);
        String captchaCode = request.getParameter(PARAM_CAPTCHA_CODE);

        // 未提交验证码时返回错误
        if (!StringUtils.hasText(captchaKey) || !StringUtils.hasText(captchaCode)) {
            log.warn("密码模式验证码校验失败：验证码参数缺失");
            recordCaptchaFailure(request, "登录失败：验证码不能为空");
            writeResult(response, "验证码不能为空");
            return;
        }

        // 调用验证码服务校验（内部校验后立即删除 Redis key，一次性使用）
        boolean verified = captchaService.verify(captchaKey, captchaCode);
        if (!verified) {
            log.warn("密码模式验证码校验失败：captcha_key={}", captchaKey);
            recordCaptchaFailure(request, "登录失败：验证码错误");
            writeResult(response, "验证码错误，请重新输入");
            return;
        }

        // 验证通过，继续后续认证流程
        filterChain.doFilter(request, response);
    }

    /**
     * 记录验证码校验失败日志（登录前置校验失败，需留有审计痕迹）。
     * <p>
     * 此时 LoginChannelContext 尚未设置（渠道由认证 Provider 根据已认证客户端判定），
     * findUser() 反查会按默认 admin realm，portal 用户可能查不到而留空 tenantId/userId，
     * 但 username/clientIp/errorMessage 等核心审计字段仍然正确记录。
     */
    private void recordCaptchaFailure(HttpServletRequest request, String errorMessage) {
        String username = request.getParameter(PARAM_USERNAME);
        loginLogPublisher.publishFailureLog(username, errorMessage);
    }

    /**
     * 判断是否为 token 端点 POST 请求。
     */
    private boolean isTokenEndpointPost(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && TOKEN_URI.equals(request.getServletPath());
    }

    /**
     * 写入统一的 {@link Result} 格式错误响应（HTTP 200，业务码 FAILURE）。
     * <p>
     * 登录失败（验证码错误、用户名密码错误等）属于业务校验失败，不是 HTTP 协议层错误，
     * 返回 200 + Result 业务码更友好，前端统一通过 {@code code} 判断成功/失败。
     */
    private void writeResult(HttpServletResponse response, String message) throws IOException {
        Result<Void> result = Result.of(ResultCode.FAILURE.getCode(), message, null, ResultType.FAILURE);
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonMapper.writeValueAsString(result));
    }
}
