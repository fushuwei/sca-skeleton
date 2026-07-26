package io.github.fushuwei.scaskeleton.auth.security;

import io.github.fushuwei.scaskeleton.security.user.ScaUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * 监听密码模式登录成功/失败事件，驱动 {@link LoginAttemptService}。
 * <p>
 * 事件由 {@link io.github.fushuwei.scaskeleton.auth.grant.base.OAuth2ResourceOwnerBaseAuthenticationProvider}
 * 直接发布（携带原始 {@link UsernamePasswordAuthenticationToken}），在 finally 清理 ThreadLocal 之前触发。
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
        Authentication authentication = event.getAuthentication();
        // 仅处理表单登录失败事件，过滤 OAuth2 客户端认证、refresh_token 续期等非用户登录场景
        // （这些事件的 authentication 是 OAuth2ClientAuthenticationToken / OAuth2RefreshTokenAuthenticationToken，其 getName() 返回 client_id，不应记入登录日志）
        if (!(authentication instanceof UsernamePasswordAuthenticationToken)) {
            return;
        }
        // 从失败认证对象读取用户名
        String username = authentication.getName();
        LoginChannel channel = LoginChannelContext.get();
        loginAttemptService.onLoginFailure(username, channel);
    }
}
