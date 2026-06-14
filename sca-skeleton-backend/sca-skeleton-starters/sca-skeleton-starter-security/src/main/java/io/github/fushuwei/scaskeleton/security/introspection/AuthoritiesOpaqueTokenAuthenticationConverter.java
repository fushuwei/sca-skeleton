package io.github.fushuwei.scaskeleton.security.introspection;

import io.github.fushuwei.scaskeleton.security.constant.OAuth2AccessTokenClaimNames;
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
 * 不透明令牌自省认证转换器
 * <p>
 * 将自省结果中的权限声明转为 {@link GrantedAuthority}，供 {@code @PreAuthorize("hasAuthority('...')")} 使用
 *
 * @author Fu Wei
 */
public class AuthoritiesOpaqueTokenAuthenticationConverter implements OpaqueTokenAuthenticationConverter {

    /**
     * 将自省主体与原始 bearer token 值包装为 {@link BearerTokenAuthentication}，并附加权限集合
     *
     * @param introspectedToken 请求中携带的 access_token 字符串
     * @param principal         自省端点解析后的主体（含 attributes）
     * @return 已认证的 BearerTokenAuthentication
     */
    @Override
    public AbstractAuthenticationToken convert(String introspectedToken, OAuth2AuthenticatedPrincipal principal) {
        // 从 authorities 声明构造 GrantedAuthority 列表，供方法级鉴权使用
        Collection<GrantedAuthority> authorities = extractAuthorities(principal);
        // 构造 BearerTokenAuthentication，token 时间与 scope 由自省 claims 承载
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER, introspectedToken, null, null, Collections.emptySet());
        return new BearerTokenAuthentication(principal, accessToken, authorities);
    }

    /**
     * 从 introspection principal 中读取 authorities：支持集合或 JSON 数组反序列化后的 List
     *
     * @param principal 自省主体
     * @return 非 null 的权限集合（可能为空）
     */
    private static Collection<GrantedAuthority> extractAuthorities(OAuth2AuthenticatedPrincipal principal) {
        Object raw = principal.getAttribute(OAuth2AccessTokenClaimNames.AUTHORITIES);
        if (raw == null) {
            return Collections.emptyList();
        }
        // authorities 为集合时逐项转为 SimpleGrantedAuthority
        if (raw instanceof Collection<?> coll) {
            List<GrantedAuthority> list = new ArrayList<>();
            for (Object o : coll) {
                if (o != null) {
                    list.add(new SimpleGrantedAuthority(Objects.toString(o)));
                }
            }
            return list;
        }
        // 单个字符串权限
        if (raw instanceof String s && !s.isEmpty()) {
            return List.of(new SimpleGrantedAuthority(s));
        }
        return Collections.emptyList();
    }
}
