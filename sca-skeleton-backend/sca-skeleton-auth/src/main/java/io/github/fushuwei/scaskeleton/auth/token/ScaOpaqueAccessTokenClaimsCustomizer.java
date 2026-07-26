package io.github.fushuwei.scaskeleton.auth.token;

import io.github.fushuwei.scaskeleton.security.user.ScaUserDetails;
import io.github.fushuwei.scaskeleton.security.constant.OAuth2AccessTokenClaimNames;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimsContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

/**
 * 不透明访问令牌的 claims 扩展：在 SAS {@link org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator}
 * 生成 reference token 时写入业务字段（sub、username、tenant_id、authorities 等），供资源服务器本地自省使用。
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
        // 1) 仅处理 access_token，refresh_token / id_token 等类型跳过
        if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
            return;
        }
        // 2) 从认证主体中提取 ScaUserDetails，非用户登录场景（如 client_credentials）跳过
        Authentication principal = context.getPrincipal();
        Object principalObj = principal.getPrincipal();
        if (!(principalObj instanceof ScaUserDetails userDetails)) {
            return;
        }
        // 3) 写入标准业务 claims，字段名与资源服务器 OAuth2AccessTokenClaimNames 约定一致
        // SAS 的 claim() 方法不允许 null 值，需逐个判空
        var claims = context.getClaims();
        if (userDetails.getUserId() != null) {
            claims.subject(userDetails.getUserId());
        }
        if (userDetails.getUsername() != null) {
            claims.claim(OAuth2AccessTokenClaimNames.PREFERRED_USERNAME, userDetails.getUsername());
        }
        if (userDetails.getTenantId() != null) {
            claims.claim(OAuth2AccessTokenClaimNames.TENANT_ID, userDetails.getTenantId());
        }
        if (userDetails.getIsSuperadmin() != null) {
            claims.claim(OAuth2AccessTokenClaimNames.IS_SUPER_ADMIN, userDetails.getIsSuperadmin());
        }
        if (userDetails.getNickname() != null) {
            claims.claim(OAuth2AccessTokenClaimNames.NICKNAME, userDetails.getNickname());
        }
        if (userDetails.getPermissions() != null) {
            claims.claim(OAuth2AccessTokenClaimNames.AUTHORITIES, userDetails.getPermissions());
        }
    }
}
