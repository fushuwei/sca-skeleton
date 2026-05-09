package io.github.fushuwei.sca.auth.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户名密码登录请求体，含验证码票据。
 *
 * @param tenantId        租户 ID
 * @param username        登录名
 * @param password        口令
 * @param captchaTicketId 验证码票据
 * @param captchaAnswer   用户输入的验证码
 * @author Fu Wei
 */
public record LoginRequest(
        @NotBlank String tenantId,
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String captchaTicketId,
        @NotBlank String captchaAnswer) {
}
