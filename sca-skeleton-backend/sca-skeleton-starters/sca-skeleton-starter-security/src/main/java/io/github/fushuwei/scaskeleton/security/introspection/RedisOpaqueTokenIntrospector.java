package io.github.fushuwei.scaskeleton.security.introspection;

import io.github.fushuwei.scaskeleton.security.constant.OAuth2AccessTokenClaimNames;
import io.github.fushuwei.scaskeleton.security.oauth2.authorization.OAuth2AuthorizationClaimsExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 不透明令牌本地自省器
 *
 * @author Fu Wei
 */
@RequiredArgsConstructor
public class RedisOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

    /**
     * OAuth2 授权服务
     */
    private final OAuth2AuthorizationService authorizationService;

    /**
     * 本地自省方法
     *
     * @param token 用于自省的令牌（请求头中的 Bearer access_token 字符串）
     * @return OAuth2 认证主体（通过 OAuth2 协议完成身份认证后，代表当前用户身份的主体对象）
     */
    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        Assert.hasText(token, "[OAuth2 令牌自省] 访问令牌不能为空");

        // 从 Redis 授权记录解析访问令牌对应的业务 claims
        Map<String, Object> claims = OAuth2AuthorizationClaimsExtractor
            .resolveAccessTokenClaims(this.authorizationService, token);
        if (claims == null || claims.isEmpty()) {
            throw new BadOpaqueTokenException("[OAuth2 令牌自省] 无效的访问令牌");
        }

        // 补齐 RFC 7662 自省语义：active=true，并与 HTTP 自省响应字段对齐
        Map<String, Object> introspectionClaims = new LinkedHashMap<>(claims);
        introspectionClaims.put(OAuth2TokenIntrospectionClaimNames.ACTIVE, true);
        if (!introspectionClaims.containsKey(OAuth2TokenIntrospectionClaimNames.SUB)) {
            Object sub = claims.get(OAuth2AccessTokenClaimNames.SUB);
            if (sub != null) {
                introspectionClaims.put(OAuth2TokenIntrospectionClaimNames.SUB, sub);
            }
        }
        String principalName = resolvePrincipalName(introspectionClaims);
        Collection<GrantedAuthority> authorities = extractAuthorities(introspectionClaims);
        return new RedisOAuth2AuthenticatedPrincipal(principalName, introspectionClaims, authorities);
    }

    /**
     * 确定 Spring Security 主体名：优先 {@code sub}，其次 {@code preferred_username}
     *
     * @param claims 自省 claim Map
     * @return 非空主体名
     */
    private static String resolvePrincipalName(Map<String, Object> claims) {
        Object sub = claims.get(OAuth2AccessTokenClaimNames.SUB);
        if (sub != null && StringUtils.hasText(sub.toString())) {
            return sub.toString();
        }
        // sub 缺失时回退 preferred_username
        Object username = claims.get(OAuth2AccessTokenClaimNames.PREFERRED_USERNAME);
        if (username != null && StringUtils.hasText(username.toString())) {
            return username.toString();
        }
        return "unknown";
    }

    /**
     * 从自省 claims 中提取权限声明，并构造 GrantedAuthority 列表
     *
     * @param claims 自省 claim Map
     * @return GrantedAuthority 列表
     */
    private static Collection<GrantedAuthority> extractAuthorities(Map<String, Object> claims) {
        Object raw = claims.get(OAuth2AccessTokenClaimNames.AUTHORITIES);
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
