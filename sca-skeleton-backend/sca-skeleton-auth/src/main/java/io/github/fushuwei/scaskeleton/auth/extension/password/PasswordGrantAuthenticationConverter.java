package io.github.fushuwei.scaskeleton.auth.extension.password;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 自定义密码授权模式请求转换器。
 * <p>
 * 负责从 {@code POST /oauth2/token} 请求中提取 {@code grant_type=password} 所需参数，
 * 并构造 {@link PasswordGrantAuthenticationToken}，交由 SAS 分发给对应的 Provider 处理。
 * <p>
 * 参数说明（均通过 HTTP Form 表单提交）：
 * <ul>
 *   <li>{@code grant_type}  = {@code password}（必须）</li>
 *   <li>{@code username}    = 登录用户名（必须）</li>
 *   <li>{@code password}    = 登录密码（必须）</li>
 *   <li>{@code tenant_id}   = 租户 ID（多租户场景必须；单租户可省略）</li>
 *   <li>{@code scope}       = 请求的权限范围（可选，空格分隔）</li>
 * </ul>
 *
 * @author Fu Wei
 */
public class PasswordGrantAuthenticationConverter implements AuthenticationConverter {

    /** 租户 ID 请求参数名 */
    private static final String PARAM_TENANT_ID = "tenant_id";

    /** RFC 6749 表单字段名（Spring Security 7 起不再提供 OAuth2ParameterNames.USERNAME/PASSWORD 常量） */
    private static final String PARAM_USERNAME = "username";

    private static final String PARAM_PASSWORD = "password";

    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!PasswordGrantAuthenticationToken.PASSWORD.getValue().equals(grantType)) {
            // 不是 password 授权类型，不处理，交由其他 Converter 继续
            return null;
        }

        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();

        String username = request.getParameter(PARAM_USERNAME);
        String password = request.getParameter(PARAM_PASSWORD);

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throwInvalidRequest(OAuth2ErrorCodes.INVALID_REQUEST,
                    "username 和 password 不能为空");
        }

        // 解析 scope（可选，空格分隔）
        Set<String> requestedScopes = new LinkedHashSet<>();
        String scope = request.getParameter(OAuth2ParameterNames.SCOPE);
        if (StringUtils.hasText(scope)) {
            requestedScopes.addAll(Arrays.asList(scope.split(" ")));
        }

        String tenantId = request.getParameter(PARAM_TENANT_ID);

        // 收集非敏感附加参数（排除标准 OAuth2 参数，避免密码泄露到日志）
        Map<String, Object> additionalParameters = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (!isStandardParameter(key) && values.length > 0) {
                additionalParameters.put(key, values[0]);
            }
        });
        // 不将密码写入 additionalParameters（安全隐患）
        additionalParameters.remove(PARAM_PASSWORD);

        return new PasswordGrantAuthenticationToken(
                clientPrincipal, requestedScopes, additionalParameters,
                username, password, tenantId);
    }

    /** 判断是否为标准 OAuth2 参数（这些参数已被专用字段处理，无需放入 additionalParameters）。 */
    private boolean isStandardParameter(String name) {
        return OAuth2ParameterNames.GRANT_TYPE.equals(name)
                || PARAM_USERNAME.equals(name)
                || PARAM_PASSWORD.equals(name)
                || OAuth2ParameterNames.SCOPE.equals(name)
                || OAuth2ParameterNames.CLIENT_ID.equals(name)
                || OAuth2ParameterNames.CLIENT_SECRET.equals(name);
    }

    private void throwInvalidRequest(String errorCode, String description) {
        throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException(
                new org.springframework.security.oauth2.core.OAuth2Error(errorCode, description, null));
    }
}
