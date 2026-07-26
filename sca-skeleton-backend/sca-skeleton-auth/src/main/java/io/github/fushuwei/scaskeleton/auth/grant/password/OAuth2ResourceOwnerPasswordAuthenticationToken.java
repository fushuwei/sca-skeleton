package io.github.fushuwei.scaskeleton.auth.grant.password;

import io.github.fushuwei.scaskeleton.auth.grant.OAuth2GrantTypeConstants;
import io.github.fushuwei.scaskeleton.auth.grant.base.OAuth2ResourceOwnerBaseAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.util.Map;
import java.util.Set;

/**
 * 密码模式认证令牌
 * <p>
 * 由 {@link OAuth2ResourceOwnerPasswordAuthenticationConverter} 从 HTTP 请求构建，
 * 传递给 {@link OAuth2ResourceOwnerPasswordAuthenticationProvider} 完成用户认证与令牌颁发。
 *
 * @author Fu Wei
 */
public class OAuth2ResourceOwnerPasswordAuthenticationToken extends OAuth2ResourceOwnerBaseAuthenticationToken {

    /**
     * 构造密码模式认证令牌。
     *
     * @param clientPrincipal       已认证的客户端主体
     * @param scopes                 请求的权限范围
     * @param additionalParameters   附加参数（含 username、password）
     */
    public OAuth2ResourceOwnerPasswordAuthenticationToken(Authentication clientPrincipal,
                                                           Set<String> scopes,
                                                           Map<String, Object> additionalParameters) {
        super(OAuth2GrantTypeConstants.PASSWORD, clientPrincipal, scopes, additionalParameters);
    }
}
