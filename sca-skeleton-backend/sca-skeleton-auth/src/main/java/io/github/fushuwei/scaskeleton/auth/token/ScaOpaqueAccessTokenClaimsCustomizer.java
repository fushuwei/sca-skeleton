package io.github.fushuwei.scaskeleton.auth.token;

import io.github.fushuwei.scaskeleton.auth.security.ScaUserDetails;
import io.github.fushuwei.scaskeleton.security.constant.OAuth2AccessTokenClaimNames;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimsContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

/**
 * 不透明访问令牌的 Claims 扩展：在 SAS {@link org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator}
 * 生成 reference token 时写入业务字段，供自省端点返回给资源服务器。
 * <p>
 * 字段名使用 {@link OAuth2AccessTokenClaimNames}，与资源服务器读取约定一致。
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
        claims.subject(userDetails.getUserId());
        claims.claim(OAuth2AccessTokenClaimNames.PREFERRED_USERNAME, userDetails.getUsername());
        claims.claim(OAuth2AccessTokenClaimNames.TENANT_ID, userDetails.getTenantId());
        claims.claim(OAuth2AccessTokenClaimNames.USER_TYPE, userDetails.getUserType());
        claims.claim(OAuth2AccessTokenClaimNames.NICKNAME, userDetails.getNickname());
        claims.claim(OAuth2AccessTokenClaimNames.PERMISSIONS, userDetails.getPermissions());
    }
}
