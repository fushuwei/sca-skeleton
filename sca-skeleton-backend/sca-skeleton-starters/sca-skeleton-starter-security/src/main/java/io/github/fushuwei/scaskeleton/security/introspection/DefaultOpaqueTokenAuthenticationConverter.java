package io.github.fushuwei.scaskeleton.security.introspection;

import io.github.fushuwei.scaskeleton.security.constant.OAuth2AccessTokenClaimNames;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenAuthenticationConverter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 不透明令牌自省认证转换器
 * <p>
 * 将自省结果中的权限声明转为 {@link GrantedAuthority}，供 {@code @PreAuthorize("hasAuthority('...')")} 使用
 *
 * @author Fu Wei
 */
public class DefaultOpaqueTokenAuthenticationConverter implements OpaqueTokenAuthenticationConverter {

    /**
     * 将原始 bearer token 与自省结果包装为 {@link BearerTokenAuthentication}，并附加权限集合
     *
     * @param introspectedToken 请求中携带的访问令牌（原始的 bearer token）
     * @param principal         自省端点解析后的主体（自省结果）
     * @return 已认证的 BearerTokenAuthentication
     */
    @Override
    public Authentication convert(String introspectedToken, OAuth2AuthenticatedPrincipal principal) {
        // 从自省结果中提取权限声明，并构造 GrantedAuthority 列表
        Collection<GrantedAuthority> authorities = extractAuthorities(principal);

        // 从自省结果中提取标准 OAuth2 令牌时间戳
        Instant issuedAt = principal.getAttribute(OAuth2AccessTokenClaimNames.IAT);
        Instant expiresAt = principal.getAttribute(OAuth2AccessTokenClaimNames.EXP);

        // 创建已验证的访问令牌
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER, introspectedToken, issuedAt, expiresAt, Collections.emptySet());

        return new BearerTokenAuthentication(principal, accessToken, authorities);
    }

    /**
     * 从自省结果中提取权限声明，并构造 GrantedAuthority 列表
     *
     * @param principal 自省端点解析后的主体（自省结果）
     * @return GrantedAuthority 列表
     */
    private static Collection<GrantedAuthority> extractAuthorities(OAuth2AuthenticatedPrincipal principal) {
        Object raw = principal.getAttribute(OAuth2AccessTokenClaimNames.AUTHORITIES);
        if (raw == null) {
            return Collections.emptyList();
        }
        // 权限为集合
        if (raw instanceof Collection<?> coll) {
            List<GrantedAuthority> list = new ArrayList<>();
            for (Object o : coll) {
                if (o != null) {
                    list.add(new SimpleGrantedAuthority(Objects.toString(o)));
                }
            }
            return list;
        }
        // 权限为字符串
        if (raw instanceof String s && !s.isEmpty()) {
            return List.of(new SimpleGrantedAuthority(s));
        }
        return Collections.emptyList();
    }
}
