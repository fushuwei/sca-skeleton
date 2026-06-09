package io.github.fushuwei.scaskeleton.auth.web;

import io.github.fushuwei.scaskeleton.auth.config.properties.OAuth2ClientProperties;
import io.github.fushuwei.scaskeleton.auth.security.LoginChannel;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 登录失败处理器：根据表单 {@code loginChannel} 将用户带回对应的 admin / portal 登录页。
 *
 * @author Fu Wei
 */
@Component
@RequiredArgsConstructor
public class ChannelAwareAuthenticationFailureHandler implements AuthenticationFailureHandler {

    /** 表单字段：标识登录页来源（admin / portal） */
    private static final String PARAM_LOGIN_CHANNEL = "loginChannel";

    /** OAuth2 客户端配置，用于解析失败回跳路径 */
    private final OAuth2ClientProperties oauth2ClientProperties;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        // 读取表单提交的登录渠道，决定回到哪个登录页
        String loginChannel = request.getParameter(PARAM_LOGIN_CHANNEL);
        // 将 loginChannel 映射为 clientId 后解析经网关的对外失败回跳 URL
        String failureUrl = oauth2ClientProperties.resolveExternalLoginFailureUrl(
                loginChannelToClientId(loginChannel));
        // 302 重定向到对应登录页并携带 error 查询参数
        response.sendRedirect(failureUrl);
    }

    /**
     * 将 loginChannel 字符串映射为用于路径解析的伪 client 标识。
     * <p>
     * {@link OAuth2ClientProperties#resolveLoginFailurePath(String)} 内部通过 portal clientId 比较实现分支。
     *
     * @param loginChannel 表单 hidden 字段值
     * @return portal 渠道返回 portal clientId，否则返回 null（走 admin 默认页）
     */
    private String loginChannelToClientId(String loginChannel) {
        // portal 渠道时使用 portal 的 clientId 触发门户登录页
        if (LoginChannel.PORTAL.getValue().equals(loginChannel)) {
            return oauth2ClientProperties.getPortal().getClientId();
        }
        // admin 或其它情况返回 admin clientId
        return oauth2ClientProperties.getAdmin().getClientId();
    }
}
