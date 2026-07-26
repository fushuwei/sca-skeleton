package io.github.fushuwei.scaskeleton.auth.grant.base;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 自定义授权模式基础认证转换器
 * <p>
 * 从 HTTP 请求中提取 {@code grant_type}、{@code scope} 和附加参数，
 * 构建对应子模式的 {@link OAuth2ResourceOwnerBaseAuthenticationToken}。
 * <p>
 * 子类需实现 {@link #support(String)} 判断是否处理指定 grant_type，
 * 以及 {@link #buildToken(Authentication, Set, Map)} 构建具体令牌。
 * <p>
 * 不依赖 SAS 内部工具类 {@code OAuth2EndpointUtils}，直接使用
 * {@link HttpServletRequest#getParameterMap()} 提取参数，避免 SAS 版本升级导致 API 不兼容。
 *
 * @author Fu Wei
 */
@Slf4j
public abstract class OAuth2ResourceOwnerBaseAuthenticationConverter
        <T extends OAuth2ResourceOwnerBaseAuthenticationToken>
        implements AuthenticationConverter {

    /**
     * 判断当前转换器是否支持指定的 grant_type。
     *
     * @param grantType 请求中的 grant_type 参数值
     * @return true 表示当前转换器处理此 grant_type
     */
    public abstract boolean support(String grantType);

    /**
     * 子类可覆盖此方法对额外参数进行校验（如 username/password 非空检查）。
     * 校验失败时抛出 {@link OAuth2AuthenticationException}。
     *
     * @param parameters 请求参数（MultiValueMap 形式）
     */
    @SuppressWarnings("unused")
    public void checkParams(MultiValueMap<String, String> parameters) {
        // 默认无额外校验，子类按需覆盖
    }

    /**
     * 构建具体子模式的 AuthenticationToken。
     *
     * @param clientPrincipal       已认证的客户端主体
     * @param requestedScopes       请求的权限范围
     * @param additionalParameters   附加参数
     * @return 子模式对应的 AuthenticationToken
     */
    public abstract T buildToken(Authentication clientPrincipal,
                                  Set<String> requestedScopes,
                                  Map<String, Object> additionalParameters);

    @Override
    public final Authentication convert(HttpServletRequest request) {
        // 1) 读取 grant_type，判断是否由当前转换器处理
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!support(grantType)) {
            return null;
        }

        log.debug("自定义授权转换器开始处理：grant_type={}, client_id={}",
                grantType, request.getParameter(OAuth2ParameterNames.CLIENT_ID));

        // 2) 提取所有请求参数为 MultiValueMap
        MultiValueMap<String, String> parameters = extractParameters(request);

        // 3) 校验 scope（OPTIONAL）
        String scope = parameters.getFirst(OAuth2ParameterNames.SCOPE);
        if (StringUtils.hasText(scope) && parameters.get(OAuth2ParameterNames.SCOPE).size() != 1) {
            throwError(OAuth2ErrorCodes.INVALID_REQUEST, OAuth2ParameterNames.SCOPE);
        }
        Set<String> requestedScopes = null;
        if (StringUtils.hasText(scope)) {
            requestedScopes = new HashSet<>(
                    Arrays.asList(StringUtils.delimitedListToStringArray(scope, " ")));
        }

        // 4) 子类参数校验（如 username / password 非空）
        checkParams(parameters);

        // 5) 从 SecurityContext 获取已认证的客户端主体
        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
        if (clientPrincipal == null) {
            throwError("invalid_client", OAuth2ParameterNames.CLIENT_ID);
        }

        // 6) 提取附加参数（排除 grant_type 和 scope 之外的所有参数）
        Map<String, Object> additionalParameters = parameters.entrySet().stream()
                .filter(e -> !OAuth2ParameterNames.GRANT_TYPE.equals(e.getKey())
                        && !OAuth2ParameterNames.SCOPE.equals(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (Object) e.getValue().get(0)));

        // 7) 构建并返回子模式令牌
        return buildToken(clientPrincipal, requestedScopes, additionalParameters);
    }

    /**
     * 从 HttpServletRequest 提取参数为 MultiValueMap。
     * <p>
     * 直接使用 {@link HttpServletRequest#getParameterMap()}，不依赖 SAS 内部工具类。
     */
    private MultiValueMap<String, String> extractParameters(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>(parameterMap.size());
        parameterMap.forEach((key, values) -> {
            if (values != null) {
                for (String value : values) {
                    parameters.add(key, value);
                }
            }
        });
        return parameters;
    }

    /**
     * 抛出标准 OAuth2 认证异常。
     *
     * @param errorCode    OAuth2 错误码（如 invalid_request、invalid_client）
     * @param parameterName 出错参数名（用于 error_description）
     */
    protected void throwError(String errorCode, String parameterName) {
        throw new OAuth2AuthenticationException(new OAuth2Error(
                errorCode,
                "OAuth 2.0 Parameter: " + parameterName,
                "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2"));
    }
}
