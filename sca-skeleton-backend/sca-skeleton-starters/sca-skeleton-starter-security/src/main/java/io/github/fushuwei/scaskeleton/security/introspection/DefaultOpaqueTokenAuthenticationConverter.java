package io.github.fushuwei.scaskeleton.security.introspection;

import io.github.fushuwei.scaskeleton.security.constant.OAuth2AccessTokenClaimNames;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenAuthenticationConverter;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;

/**
 * 不透明令牌身份认证转换器
 *
 * @author Fu Wei
 */
public class DefaultOpaqueTokenAuthenticationConverter implements OpaqueTokenAuthenticationConverter {

    /**
     * 身份认证转换，将自省结果与权限集合包装为 {@link BearerTokenAuthentication}
     *
     * @param introspectedToken 请求中携带的访问令牌（原始的 bearer token）
     * @param principal         自省端点解析后的主体（自省结果）
     * @return 已认证的 BearerTokenAuthentication
     */
    @Override
    @SuppressWarnings("unchecked")
    public Authentication convert(String introspectedToken, OAuth2AuthenticatedPrincipal principal) {
        // 获取权限列表
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) principal.getAuthorities();

        // 从自省结果中提取标准 OAuth2 令牌时间戳
        Instant issuedAt = principal.getAttribute(OAuth2AccessTokenClaimNames.IAT);
        Instant expiresAt = principal.getAttribute(OAuth2AccessTokenClaimNames.EXP);

        // 创建已验证的访问令牌
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER, introspectedToken, issuedAt, expiresAt, Collections.emptySet());

        return new BearerTokenAuthentication(principal, accessToken, authorities);
    }
}
