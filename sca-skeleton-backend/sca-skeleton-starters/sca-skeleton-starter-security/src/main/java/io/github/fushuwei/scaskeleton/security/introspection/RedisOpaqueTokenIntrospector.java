package io.github.fushuwei.scaskeleton.security.introspection;

import io.github.fushuwei.scaskeleton.security.constant.OAuth2AccessTokenClaimNames;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.Instant;
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

        // 从 Redis 授权记录中解析访问令牌对应的业务 claims
        Map<String, Object> claims = extractAccessTokenClaims(this.authorizationService, token);
        if (claims == null || claims.isEmpty()) {
            throw new BadOpaqueTokenException("[OAuth2 令牌自省] 无效的访问令牌");
        }

        String principalName = extractPrincipalName(claims);
        Collection<GrantedAuthority> authorities = extractAuthorities(claims);
        return new RedisOAuth2AuthenticatedPrincipal(principalName, claims, authorities);
    }

    /**
     * 从 Redis 授权记录中解析访问令牌对应的业务 claims
     *
     * @param authorizationService OAuth2 授权服务
     * @param token                访问令牌
     * @return claims 集合
     */
    @Nullable
    private static Map<String, Object> extractAccessTokenClaims(OAuth2AuthorizationService authorizationService,
                                                                String token) {
        // 通过 access_token 令牌查询 Redis 授权记录
        OAuth2Authorization authorization = authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
        if (authorization == null) {
            return null;
        }

        // 获取授权记录中 OAuth2AccessToken 类型的令牌
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        if (accessToken == null || accessToken.getToken() == null) {
            return null;
        }

        // 判断令牌是否过期
        if (accessToken.getToken().getExpiresAt() != null
            && Instant.now().isAfter(accessToken.getToken().getExpiresAt())) {
            return null;
        }

        // 获取业务 claims
        Object claimsObj = accessToken.getMetadata().get(OAuth2Authorization.Token.CLAIMS_METADATA_NAME);
        if (!(claimsObj instanceof Map<?, ?> rawClaims) || rawClaims.isEmpty()) {
            return null;
        }

        Map<String, Object> claims = new LinkedHashMap<>();

        // 循环获取到的原始 claims，过滤 null 键值
        for (Map.Entry<?, ?> entry : rawClaims.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                claims.put(entry.getKey().toString(), entry.getValue());
            }
        }

        // 补充令牌签发时间
        if (accessToken.getToken().getIssuedAt() != null) {
            claims.put(OAuth2AccessTokenClaimNames.IAT, accessToken.getToken().getIssuedAt());
        }
        // 补充令牌过期时间
        if (accessToken.getToken().getExpiresAt() != null) {
            claims.put(OAuth2AccessTokenClaimNames.EXP, accessToken.getToken().getExpiresAt());
        }

        // 补充令牌有效状态（能通过过期检查走到这里，即令牌有效）
        claims.put(OAuth2TokenIntrospectionClaimNames.ACTIVE, true);

        return claims;
    }

    /**
     * 从自省 claims 中提取主体名称
     *
     * @param claims 自省 claim 集合
     * @return 主体名称
     */
    private static String extractPrincipalName(Map<String, Object> claims) {
        // 获取 sub
        Object sub = claims.get(OAuth2AccessTokenClaimNames.SUB);
        if (sub != null && StringUtils.hasText(sub.toString())) {
            return sub.toString();
        }

        // 如果没有 sub，则使用 preferred_username
        Object username = claims.get(OAuth2AccessTokenClaimNames.PREFERRED_USERNAME);
        if (username != null && StringUtils.hasText(username.toString())) {
            return username.toString();
        }

        return "unknown";
    }

    /**
     * 从自省 claims 中提取权限声明，并构造 GrantedAuthority 列表
     *
     * @param claims 自省 claim 集合
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
