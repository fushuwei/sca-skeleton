package io.github.fushuwei.sca.auth.token;

import io.github.fushuwei.sca.auth.security.ScaUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimsContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

/**
 * 不透明访问令牌的 Claims 扩展：在 SAS {@link org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator}
 * 生成 reference token 时写入业务字段，供自省端点返回给资源服务器。
 * <p>
 * 字段与原先 JWT access_token 保持一致：{@code sub}（用户 ID）、{@code preferred_username}、{@code tenant_id}、
 * {@code user_type}、{@code nickname}、{@code permissions}。
 *
 * @author Fu Wei
 */
public class ScaOpaqueAccessTokenClaimsCustomizer implements OAuth2TokenCustomizer<OAuth2TokenClaimsContext> {

    /**
     * 仅处理 access_token 类型；其它 token 类型直接跳过。
     *
     * @param context SAS 传入的 claims 构建上下文
     */
    @Override
    public void customize(OAuth2TokenClaimsContext context) {
        if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
            return;
        }
        Authentication principal = context.getPrincipal();
        Object principalObj = principal.getPrincipal();
        if (!(principalObj instanceof ScaUserDetails userDetails)) {
            return;
        }
        var claims = context.getClaims();
        // sub 存业务用户 ID（覆盖生成器默认的 principal.getName() 即登录名）
        claims.subject(userDetails.getUserId());
        claims.claim("preferred_username", userDetails.getUsername());
        claims.claim("tenant_id", userDetails.getTenantId());
        claims.claim("user_type", userDetails.getUserType());
        claims.claim("nickname", userDetails.getNickname());
        claims.claim("permissions", userDetails.getPermissions());
    }
}
