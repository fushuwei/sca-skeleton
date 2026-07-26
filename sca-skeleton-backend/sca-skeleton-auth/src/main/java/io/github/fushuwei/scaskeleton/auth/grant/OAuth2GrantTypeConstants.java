package io.github.fushuwei.scaskeleton.auth.grant;

import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * OAuth2 自定义授权类型常量
 * <p>
 * Spring Authorization Server 原生支持 {@code authorization_code}、{@code refresh_token}、
 * {@code client_credentials} 等标准授权类型。本类定义项目扩展的自定义授权类型，
 * 用于在 SAS 中注册并驱动自定义的 {@code AuthenticationConverter} 与 {@code AuthenticationProvider}。
 *
 * @author Fu Wei
 */
public final class OAuth2GrantTypeConstants {

    private OAuth2GrantTypeConstants() {
    }

    /**
     * 密码模式（Resource Owner Password Credentials Grant）
     * <p>
     * RFC 6749 Section 4.3 定义的标准授权类型，OAuth 2.1 中已废弃，
     * 但在国内管理系统中仍是主流的认证模式。
     */
    public static final AuthorizationGrantType PASSWORD = new AuthorizationGrantType("password");
}
