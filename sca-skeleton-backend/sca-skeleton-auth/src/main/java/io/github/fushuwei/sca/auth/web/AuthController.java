package io.github.fushuwei.sca.auth.web;

import io.github.fushuwei.sca.auth.service.AuthTokenService;
import io.github.fushuwei.sca.auth.service.CaptchaTicketService;
import io.github.fushuwei.sca.auth.web.dto.LoginRequest;
import io.github.fushuwei.sca.auth.web.dto.RefreshTokenRequest;
import io.github.fushuwei.sca.auth.web.dto.TokenResponse;
import io.github.fushuwei.sca.starter.web.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 对外认证 API：验证码、登录、刷新。
 *
 * @author Fu Wei
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    // 验证码签发。
    private final CaptchaTicketService captchaTicketService;
    // 令牌签发。
    private final AuthTokenService authTokenService;

    // 获取图形验证码。
    @PostMapping("/captcha")
    public ApiResponse<Map<String, String>> captcha() {
        // 生成票据与图片。
        CaptchaTicketService.CaptchaTicket ticket = captchaTicketService.issue();
        // 返回票据 ID 与 data URL。
        return ApiResponse.success(Map.of(
                "ticketId", ticket.ticketId(),
                "image", ticket.image()));
    }

    // 用户名密码登录。
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        // 先校验验证码，再执行认证。
        captchaTicketService.validateAndConsume(request.captchaTicketId(), request.captchaAnswer());
        // 认证并签发令牌。
        AuthTokenService.TokenPair pair = authTokenService.login(
                request.tenantId(),
                request.username(),
                request.password());
        // 组装标准响应。
        return ApiResponse.success(toResponse(pair));
    }

    // 刷新访问令牌。
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        // 使用刷新令牌换新对。
        AuthTokenService.TokenPair pair = authTokenService.refresh(request.refreshToken());
        return ApiResponse.success(toResponse(pair));
    }

    // 映射内部令牌对到对外 DTO。
    private static TokenResponse toResponse(AuthTokenService.TokenPair pair) {
        // token_type 固定 Bearer。
        return new TokenResponse(pair.accessToken(), pair.refreshToken(), "Bearer", pair.expiresIn());
    }
}
