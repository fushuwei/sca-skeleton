package io.github.fushuwei.scaskeleton.auth.security;

import io.github.fushuwei.scaskeleton.security.user.ScaUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * 监听表单登录成功/失败事件，驱动 {@link LoginAttemptService}。
 *
 * @author Fu Wei
 */
@Component
@RequiredArgsConstructor
public class LoginAttemptEventListener {

    /** 登录失败计数与锁定逻辑 */
    private final LoginAttemptService loginAttemptService;

    /**
     * 登录成功后清零失败计数。
     */
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        // 仅处理已加载为 ScaUserDetails 的表单登录成功事件
        Authentication authentication = event.getAuthentication();
        if (!(authentication.getPrincipal() instanceof ScaUserDetails details)) {
            return;
        }
        // 读取 Filter 写入的登录渠道
        LoginChannel channel = LoginChannelContext.get();
        loginAttemptService.onLoginSuccess(details.getUsername(), channel);
    }

    /**
     * 登录失败后递增失败计数，必要时锁定账号。
     */
    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        // 从失败认证对象读取用户名
        String username = event.getAuthentication().getName();
        LoginChannel channel = LoginChannelContext.get();
        loginAttemptService.onLoginFailure(username, channel);
    }
}
