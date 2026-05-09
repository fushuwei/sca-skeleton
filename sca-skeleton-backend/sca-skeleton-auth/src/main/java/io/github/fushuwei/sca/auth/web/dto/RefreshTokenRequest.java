package io.github.fushuwei.sca.auth.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新访问令牌请求体。
 *
 * @param refreshToken 刷新令牌
 * @author Fu Wei
 */
public record RefreshTokenRequest(@NotBlank String refreshToken) {
}
