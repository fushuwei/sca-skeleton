package io.github.fushuwei.sca.starter.security.introspection;

import io.github.fushuwei.sca.oauth2.redis.OAuth2AuthorizationRedisReader;
import io.github.fushuwei.sca.oauth2.redis.OAuth2TokenClaimNames;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于 Redis 的不透明令牌自省器。
 * <p>
 * 直接读取认证中心写入的 OAuth2 授权记录（与 {@code RedisOAuth2AuthorizationService} 同一 Redis），
 * 不走 HTTP {@code /oauth2/introspect}，避免每请求多一次网络往返。
 * <p>
 * 返回的 claim 集合与 {@link SpringOpaqueTokenIntrospector} HTTP 自省语义对齐，
 * 供 {@link PermissionsOpaqueTokenAuthenticationConverter} 与 {@code @PreAuthorize} 使用。
 *
 * @author Fu Wei
 */
public class RedisOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

    /**
     * 只读 Redis 授权访问器，由自动配置注入。
     */
    private final OAuth2AuthorizationRedisReader authorizationRedisReader;

    /**
     * @param authorizationRedisReader Redis 授权读模型，不可为 null
     */
    public RedisOpaqueTokenIntrospector(OAuth2AuthorizationRedisReader authorizationRedisReader) {
        Assert.notNull(authorizationRedisReader, "authorizationRedisReader cannot be null");
        this.authorizationRedisReader = authorizationRedisReader;
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
        Map<String, Object> claims = this.authorizationRedisReader.resolveAccessTokenClaims(token);
        if (claims == null || claims.isEmpty()) {
            throw new BadOpaqueTokenException("Invalid access token");
        }
        // 补齐 RFC 7662 自省标准字段，与 HTTP 自省响应行为一致
        Map<String, Object> introspectionClaims = new LinkedHashMap<>(claims);
        introspectionClaims.put(OAuth2TokenIntrospectionClaimNames.ACTIVE, true);
        if (!introspectionClaims.containsKey(OAuth2TokenIntrospectionClaimNames.SUB)) {
            Object sub = claims.get(OAuth2TokenClaimNames.SUB);
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
        Object sub = claims.get(OAuth2TokenClaimNames.SUB);
        if (sub != null && StringUtils.hasText(sub.toString())) {
            return sub.toString();
        }
        Object username = claims.get(OAuth2TokenClaimNames.PREFERRED_USERNAME);
        if (username != null && StringUtils.hasText(username.toString())) {
            return username.toString();
        }
        return "unknown";
    }
}
