package io.github.fushuwei.scaskeleton.security.oauth2;

import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;

import java.time.Instant;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 从 {@link OAuth2Authorization} 提取不透明 access_token 自省所需的 claims。
 *
 * @author Fu Wei
 */
public final class OAuth2AuthorizationClaimsExtractor {

    private OAuth2AuthorizationClaimsExtractor() {
    }

    /**
     * 按 access_token 明文解析业务 claims（与 HTTP {@code /oauth2/introspect} 语义一致）。
     *
     * @param authorizationService Redis 授权服务
     * @param accessTokenValue     Bearer 令牌值
     * @return claims；无效或已过期时返回 null
     */
    @Nullable
    public static Map<String, Object> resolveAccessTokenClaims(
            OAuth2AuthorizationService authorizationService,
            String accessTokenValue) {
        // 按 access_token 类型查 Redis 授权记录
        OAuth2Authorization authorization = authorizationService.findByToken(accessTokenValue,
                OAuth2TokenType.ACCESS_TOKEN);
        if (authorization == null) {
            return null;
        }
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        if (accessToken == null || accessToken.getToken() == null) {
            return null;
        }
        // 已过期视为无效令牌
        if (accessToken.getToken().getExpiresAt() != null
                && Instant.now().isAfter(accessToken.getToken().getExpiresAt())) {
            return null;
        }
        // 业务 claims 存放在 access_token metadata 中
        Object claimsObj = accessToken.getMetadata().get(OAuth2Authorization.Token.CLAIMS_METADATA_NAME);
        if (!(claimsObj instanceof Map<?, ?> rawClaims) || rawClaims.isEmpty()) {
            return null;
        }
        // 过滤 null 键值，保持插入顺序
        Map<String, Object> claims = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawClaims.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                claims.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return claims.isEmpty() ? null : claims;
    }
}
