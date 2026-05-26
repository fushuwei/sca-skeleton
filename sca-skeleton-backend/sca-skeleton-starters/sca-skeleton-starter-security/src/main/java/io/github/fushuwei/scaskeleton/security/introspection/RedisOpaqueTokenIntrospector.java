package io.github.fushuwei.scaskeleton.security.introspection;

import io.github.fushuwei.scaskeleton.security.constant.OAuth2AccessTokenClaimNames;
import io.github.fushuwei.scaskeleton.security.oauth2.OAuth2AuthorizationClaimsExtractor;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于 {@link RedisOAuth2AuthorizationService} 的不透明令牌自省器。
 * <p>
 * 通过标准 {@link org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService#findByToken}
 * 读取授权记录，不走 HTTP {@code /oauth2/introspect}。
 * <p>
 * 返回的 claim 集合与 {@link SpringOpaqueTokenIntrospector} HTTP 自省语义对齐，
 * 供 {@link PermissionsOpaqueTokenAuthenticationConverter} 与 {@code @PreAuthorize} 使用。
 *
 * @author Fu Wei
 */
public class RedisOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

    /**
     * Redis 版 OAuth2 授权服务。
     */
    private final OAuth2AuthorizationService authorizationService;

    /**
     * @param authorizationService Redis 版 OAuth2 授权服务，不可为 null
     */
    public RedisOpaqueTokenIntrospector(OAuth2AuthorizationService authorizationService) {
        Assert.notNull(authorizationService, "authorizationService cannot be null");
        this.authorizationService = authorizationService;
    }

    /**
     * 在 Redis 中解析 access_token 并构造 {@link OAuth2AuthenticatedPrincipal}。
     *
     * @param token Bearer access_token 明文
     * @return 已激活的主体；无效时抛出 {@link BadOpaqueTokenException}
     */
    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        Assert.hasText(token, "token cannot be empty");
        // 从 Redis 授权记录解析 access_token 业务 claims
        Map<String, Object> claims = OAuth2AuthorizationClaimsExtractor.resolveAccessTokenClaims(
                this.authorizationService, token);
        if (claims == null || claims.isEmpty()) {
            throw new BadOpaqueTokenException("Invalid access token");
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
        return new RedisOAuth2AuthenticatedPrincipal(principalName, introspectionClaims);
    }

    /**
     * 确定 Spring Security 主体名：优先 {@code sub}，其次 {@code preferred_username}。
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
}
