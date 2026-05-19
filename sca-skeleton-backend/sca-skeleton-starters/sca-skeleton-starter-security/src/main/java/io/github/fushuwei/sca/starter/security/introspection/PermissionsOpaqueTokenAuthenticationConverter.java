package io.github.fushuwei.sca.starter.security.introspection;

import io.github.fushuwei.sca.starter.security.constant.OAuth2AccessTokenClaimNames;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenAuthenticationConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 不透明令牌自省认证转换器：将 introspection 返回的 {@code permissions} 声明转为 {@link GrantedAuthority}，
 * 与网关侧 JWT 时代行为一致（权限码无前缀，供 {@code @PreAuthorize("hasAuthority('...')")} 使用）。
 *
 * @author Fu Wei
 */
public class PermissionsOpaqueTokenAuthenticationConverter implements OpaqueTokenAuthenticationConverter {

    /**
     * 自省权限声明在 token 属性中的键名，与认证服务写入的 access_token claims 一致。
     */
    private static final String PERMISSIONS_CLAIM = OAuth2AccessTokenClaimNames.PERMISSIONS;

    /**
     * 将自省主体与原始 bearer token 值包装为 {@link BearerTokenAuthentication}，并附加权限集合。
     *
     * @param introspectedToken 请求中携带的 access_token 字符串
     * @param principal         自省端点解析后的主体（含 attributes）
     * @return 已认证的 BearerTokenAuthentication
     */
    @Override
    public AbstractAuthenticationToken convert(String introspectedToken, OAuth2AuthenticatedPrincipal principal) {
        // 从 permissions 声明构造 GrantedAuthority 列表，供方法级鉴权使用
        Collection<GrantedAuthority> authorities = extractPermissionAuthorities(principal);
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, introspectedToken, null, null, Collections.emptySet());
        return new BearerTokenAuthentication(principal, accessToken, authorities);
    }

    /**
     * 从 introspection principal 中读取 permissions：支持集合或 JSON 数组反序列化后的 List。
     *
     * @param principal 自省主体
     * @return 非 null 的权限集合（可能为空）
     */
    private static Collection<GrantedAuthority> extractPermissionAuthorities(OAuth2AuthenticatedPrincipal principal) {
        Object raw = principal.getAttribute(PERMISSIONS_CLAIM);
        if (raw == null) {
            return Collections.emptyList();
        }
        if (raw instanceof Collection<?> coll) {
            List<GrantedAuthority> list = new ArrayList<>();
            for (Object o : coll) {
                if (o != null) {
                    list.add(new SimpleGrantedAuthority(Objects.toString(o)));
                }
            }
            return list;
        }
        if (raw instanceof String s && !s.isEmpty()) {
            return List.of(new SimpleGrantedAuthority(s));
        }
        return Collections.emptyList();
    }
}
