package io.github.fushuwei.sca.auth.token;

import io.github.fushuwei.sca.auth.security.ScaUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

/**
 * JWT Token 自定义扩展：向 access_token 注入业务 Claims。
 * <p>
 * SAS 生成 JWT 时回调此 Customizer，将用户的业务属性写入 Token Payload，
 * 下游资源服务（system 等）无需再查库，可直接从 JWT 解析用户上下文。
 * <p>
 * 写入的自定义 Claims（与 {@code SecurityProperties} 中的字段名对应）：
 * <ul>
 *   <li>{@code sub}               — 用户 ID（UUID，覆盖标准 sub）</li>
 *   <li>{@code preferred_username} — 登录用户名（OIDC 标准字段）</li>
 *   <li>{@code tenant_id}          — 租户 ID</li>
 *   <li>{@code user_type}          — 用户类型（superadmin / tenant_admin / normal …）</li>
 *   <li>{@code nickname}           — 昵称</li>
 *   <li>{@code permissions}        — 权限码列表（button 类型）</li>
 * </ul>
 *
 * @author Fu Wei
 */
@Slf4j
public class ScaTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    @Override
    public void customize(JwtEncodingContext context) {
        // 仅对 access_token 注入业务 claims；id_token 和 refresh_token 不做扩展
        if (!org.springframework.security.oauth2.server.authorization.OAuth2TokenType.ACCESS_TOKEN
                .equals(context.getTokenType())) {
            return;
        }

        Authentication principal = context.getPrincipal();
        Object principalObj = principal.getPrincipal();

        if (!(principalObj instanceof ScaUserDetails userDetails)) {
            return;
        }

        var claims = context.getClaims();

        // sub 字段存储用户 ID（UUID），便于资源服务通过 sub 快速定位用户
        claims.subject(userDetails.getUserId());

        // 登录用户名（OIDC preferred_username 标准字段）
        claims.claim("preferred_username", userDetails.getUsername());

        // 租户 ID：多租户场景下资源服务按此隔离数据
        claims.claim("tenant_id", userDetails.getTenantId());

        // 用户类型：资源服务可据此做粗粒度角色判断（如超管绕过某些业务限制）
        claims.claim("user_type", userDetails.getUserType());

        // 昵称：避免前端再次请求用户详情接口
        claims.claim("nickname", userDetails.getNickname());

        // 权限码列表：资源服务使用 @PreAuthorize("hasAuthority('sys:user:list')") 进行细粒度鉴权
        claims.claim("permissions", userDetails.getPermissions());
    }
}
