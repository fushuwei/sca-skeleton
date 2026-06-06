package io.github.fushuwei.scaskeleton.auth.token;

import org.springframework.lang.Nullable;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

/**
 * 自定义 refresh_token 生成器。
 * <p>
 * SAS 7.0 内置的 {@code OAuth2RefreshTokenGenerator} 会在授权码流程中
 * 对公共客户端（{@code ClientAuthenticationMethod.NONE}）直接返回 null，
 * 导致 PKCE + 公共客户端场景无法获取 refresh_token。
 * <p>
 * 本实现完全遵循 OAuth 2.1（RFC 9470）规范：当客户端启用 PKCE 且
 * 配置了 refresh token rotation（{@code reuseRefreshTokens=false}）时，
 * 允许向公共客户端签发 refresh_token。
 *
 * @author Fu Wei
 */
public class ScaRefreshTokenGenerator implements OAuth2TokenGenerator<OAuth2RefreshToken> {

    private final StringKeyGenerator refreshTokenGenerator =
            new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96);

    private Clock clock = Clock.systemUTC();

    @Nullable
    @Override
    public OAuth2RefreshToken generate(OAuth2TokenContext context) {
        if (!OAuth2TokenType.REFRESH_TOKEN.equals(context.getTokenType())) {
            return null;
        }
        Instant issuedAt = this.clock.instant();
        Instant expiresAt = issuedAt.plus(
                context.getRegisteredClient().getTokenSettings().getRefreshTokenTimeToLive());
        return new OAuth2RefreshToken(this.refreshTokenGenerator.generateKey(), issuedAt, expiresAt);
    }
}
