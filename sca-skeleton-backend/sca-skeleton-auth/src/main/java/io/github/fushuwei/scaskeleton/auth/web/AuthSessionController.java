package io.github.fushuwei.scaskeleton.auth.web;

import org.springframework.web.bind.annotation.RestController;

/**
 * Auth 统一会话控制器（占位）。
 * <p>
 * 退出逻辑已移至 {@link io.github.fushuwei.scaskeleton.auth.config.AuthSecurityConfig} 中
 * 由 Spring Security {@code LogoutFilter} 统一编排，包括令牌吊销、Session 销毁与按渠道回跳。
 *
 * @author Fu Wei
 */
@RestController
public class AuthSessionController {

    // 退出端点 /logout 已由 AuthSecurityConfig.LogoutFilter 接管，
    // 此处不再声明 @PostMapping("/logout") / @GetMapping("/logout")，
    // 避免与 SecurityFilterChain 的 LogoutFilter 产生 handler mapping 冲突。
}

