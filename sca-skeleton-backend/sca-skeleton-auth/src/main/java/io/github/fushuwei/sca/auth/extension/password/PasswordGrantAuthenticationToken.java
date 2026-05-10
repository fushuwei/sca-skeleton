package io.github.fushuwei.sca.auth.extension.password;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

import java.util.Map;
import java.util.Set;

/**
 * 自定义密码授权模式认证令牌。
 * <p>
 * 封装客户端登录请求中携带的 username、password、tenantId 等参数，
 * 由 {@link PasswordGrantAuthenticationConverter} 从 HTTP 请求中提取并构造，
 * 再交由 {@link PasswordGrantAuthenticationProvider} 验证。
 * <p>
 * 授权类型值固定为 {@code "password"}，与 OAuth 2.0 密码授权保持兼容。
 *
 * @author Fu Wei
 */
public class PasswordGrantAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

    /** 自定义授权类型：password */
    public static final AuthorizationGrantType PASSWORD =
            new AuthorizationGrantType("password");

    /** 登录用户名 */
    private final String username;

    /** 登录密码（明文，验证后不写入 SecurityContext） */
    private final String password;

    /** 租户 ID（多租户场景必传，单租户可为 null） */
    private final String tenantId;

    /**
     * @param clientPrincipal   已认证的客户端主体（由 SAS 标准客户端认证过滤器注入）
     * @param requestedScopes   客户端请求的 scope 集合
     * @param additionalParameters 请求中的附加参数（原始）
     * @param username          用户名
     * @param password          明文密码
     * @param tenantId          租户 ID
     */
    public PasswordGrantAuthenticationToken(Authentication clientPrincipal,
                                            Set<String> requestedScopes,
                                            Map<String, Object> additionalParameters,
                                            String username,
                                            String password,
                                            String tenantId) {
        super(PASSWORD, clientPrincipal, additionalParameters);
        this.username = username;
        this.password = password;
        this.tenantId = tenantId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getTenantId() {
        return tenantId;
    }
}
