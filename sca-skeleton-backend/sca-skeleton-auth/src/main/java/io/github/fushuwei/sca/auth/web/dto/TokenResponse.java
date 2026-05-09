package io.github.fushuwei.sca.auth.web.dto;

/**
 * 令牌对响应，字段命名贴近 OAuth2 访问令牌响应。
 *
 * @param accessToken  访问令牌（JWT）
 * @param refreshToken 刷新令牌（opaque，仅存 Redis）
 * @param tokenType    固定 Bearer
 * @param expiresIn    访问令牌剩余秒
 * @author Fu Wei
 */
public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
}
