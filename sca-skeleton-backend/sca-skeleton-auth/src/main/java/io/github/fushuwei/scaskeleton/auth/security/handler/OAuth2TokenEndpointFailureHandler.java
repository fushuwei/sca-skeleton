package io.github.fushuwei.scaskeleton.auth.security.handler;

import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.core.result.ResultType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * OAuth2 token 端点认证失败处理器：返回统一的 {@link Result} 格式响应（HTTP 200）。
 * <p>
 * 替代 SAS 默认的 {@code OAuth2ErrorAuthenticationFailureHandler}（返回 HTTP 400 +
 * {@code error}/{@code error_description} 标准格式），统一登录失败的业务响应格式，
 * 便于前端通过 {@code code} 判断成功/失败。
 * <p>
 * 处理场景：用户名密码错误、账号锁定/禁用/过期、scope 不合法等认证 Provider 抛出的
 * {@link OAuth2AuthenticationException}。客户端认证失败（client_id/client_secret 错误）
 * 由 {@code AuthorizationServerConfig.oauth2TokenEndpointAuthenticationEntryPoint} 处理。
 *
 * @author Fu Wei
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2TokenEndpointFailureHandler implements AuthenticationFailureHandler {

    private final JsonMapper jsonMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authenticationException) throws IOException {
        String message = resolveMessage(authenticationException);

        Result<Void> result = Result.of(ResultCode.FAILURE.getCode(), message, null, ResultType.FAILURE);
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonMapper.writeValueAsString(result));
    }

    /**
     * 从认证异常中提取用户可读的错误消息。
     * <p>
     * 优先使用 {@link OAuth2Error#getDescription()}（由
     * {@code OAuth2ResourceOwnerBaseAuthenticationProvider.mapToOAuth2AuthenticationException}
     * 设置的中文消息，如"用户名或密码错误"），缺失时回退到 errorCode，最终回退到"认证失败"。
     */
    private String resolveMessage(AuthenticationException ex) {
        if (ex instanceof OAuth2AuthenticationException oauth2Ex) {
            OAuth2Error error = oauth2Ex.getError();
            if (error != null) {
                if (StringUtils.hasText(error.getDescription())) {
                    return error.getDescription();
                }
                if (StringUtils.hasText(error.getErrorCode())) {
                    return error.getErrorCode();
                }
            }
        }
        return "认证失败";
    }
}
