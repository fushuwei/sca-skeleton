package io.github.fushuwei.scaskeleton.auth.grant.password;

import io.github.fushuwei.scaskeleton.auth.grant.OAuth2GrantTypeConstants;
import io.github.fushuwei.scaskeleton.auth.grant.base.OAuth2ResourceOwnerBaseAuthenticationConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Set;

/**
 * 密码模式认证转换器
 * <p>
 * 从 {@code POST /oauth2/token} 请求中提取 {@code grant_type=password} 的
 * {@code username} 和 {@code password} 参数，构建 {@link OAuth2ResourceOwnerPasswordAuthenticationToken}。
 *
 * @author Fu Wei
 */
@Slf4j
public class OAuth2ResourceOwnerPasswordAuthenticationConverter
        extends OAuth2ResourceOwnerBaseAuthenticationConverter<OAuth2ResourceOwnerPasswordAuthenticationToken> {

    /**
     * 请求参数名：用户名
     */
    private static final String PARAM_USERNAME = "username";

    /**
     * 请求参数名：密码
     */
    private static final String PARAM_PASSWORD = "password";

    @Override
    public boolean support(String grantType) {
        return OAuth2GrantTypeConstants.PASSWORD.getValue().equals(grantType);
    }

    @Override
    public void checkParams(MultiValueMap<String, String> parameters) {
        // username（REQUIRED）
        String username = parameters.getFirst(PARAM_USERNAME);
        if (!StringUtils.hasText(username) || parameters.get(PARAM_USERNAME).size() != 1) {
            log.warn("密码模式参数校验失败：username 缺失或重复");
            throwError(OAuth2ErrorCodes.INVALID_REQUEST, PARAM_USERNAME);
        }

        // password（REQUIRED）
        String password = parameters.getFirst(PARAM_PASSWORD);
        if (!StringUtils.hasText(password) || parameters.get(PARAM_PASSWORD).size() != 1) {
            log.warn("密码模式参数校验失败：password 缺失或重复");
            throwError(OAuth2ErrorCodes.INVALID_REQUEST, PARAM_PASSWORD);
        }
    }

    @Override
    public OAuth2ResourceOwnerPasswordAuthenticationToken buildToken(Authentication clientPrincipal,
                                                                       Set<String> requestedScopes,
                                                                       Map<String, Object> additionalParameters) {
        return new OAuth2ResourceOwnerPasswordAuthenticationToken(
                clientPrincipal, requestedScopes, additionalParameters);
    }
}
