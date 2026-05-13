package io.github.fushuwei.scaskeleton.gateway.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenAuthenticationConverter;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 网关响应式不透明令牌认证转换器：将 introspection 的 {@code permissions} 转为 GrantedAuthority。
 *
 * @author Fu Wei
 */
public class PermissionsReactiveOpaqueTokenAuthenticationConverter
        implements ReactiveOpaqueTokenAuthenticationConverter {

    /**
     * 与认证服务写入的 access_token 业务 claims 键名一致。
     */
    private static final String PERMISSIONS_CLAIM = "permissions";

    /**
     * 构造 {@link BearerTokenAuthentication} 并附加权限集合。
     *
     * @param token     请求中的 bearer token 字符串
     * @param principal 自省主体
     * @return Mono 包装的认证对象
     */
    @Override
    public Mono<Authentication> convert(String token, OAuth2AuthenticatedPrincipal principal) {
        Collection<GrantedAuthority> authorities = extractPermissionAuthorities(principal);
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, token, null, null, Collections.emptySet());
        return Mono.just(new BearerTokenAuthentication(principal, accessToken, authorities));
    }

    /**
     * 从 principal 解析 permissions 列表。
     *
     * @param principal 自省主体
     * @return 权限集合
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
