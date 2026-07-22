package io.github.fushuwei.scaskeleton.auth.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.fushuwei.scaskeleton.auth.infrastructure.entity.SysUser;
import io.github.fushuwei.scaskeleton.auth.infrastructure.mapper.SysUserMapper;
import io.github.fushuwei.scaskeleton.auth.security.filter.LoginChannelFilter;
import io.github.fushuwei.scaskeleton.log.event.LoginLogEvent;
import io.github.fushuwei.scaskeleton.log.support.UserAgentParser;
import io.github.fushuwei.scaskeleton.security.user.ScaUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 登录日志事件发布器：监听 Spring Security 登录成功/失败事件，
 * 在请求线程内同步采集 IP、User-Agent、耗时等元数据，封装为 {@link LoginLogEvent} 后发布。
 * <p>
 * 实际的异步落库由 starter-log 中的 {@link io.github.fushuwei.scaskeleton.log.event.LoginLogEventListener}
 * 在独立线程完成，本类不感知持久化细节。
 * <p>
 * 与 {@link LoginAttemptEventListener} 各司其职：后者负责失败计数与账号锁定，本类负责审计日志。
 * <p>
 * 所有 ThreadLocal 访问（{@link RequestContextHolder}、{@link LoginChannelContext}）均在本类的同步方法内完成，
 * 确保异步线程不需要访问 ThreadLocal。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>username 字段始终记录登录时输入的原始用户名（无论用户是否存在）</li>
 *   <li>成功路径：从 {@link ScaUserDetails} 直接获取 tenantId/userId，无需额外查询</li>
 *   <li>失败路径：SecurityContext 未建立，需反查 sys_user 填充 tenantId/userId（用户不存在时为空）</li>
 *   <li>real_name 由列表查询时 LEFT JOIN sys_user.real_name 获取，不在本类处理</li>
 *   <li>costMs 由 {@link LoginChannelFilter} 记录开始时间，本类在发布事件时计算差值</li>
 * </ul>
 *
 * @author Fu Wei
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginLogPublisher {

    /** 事件发布器：发布 {@link LoginLogEvent} 触发异步落库 */
    private final ApplicationEventPublisher eventPublisher;

    /** auth 专属用户 Mapper：失败时反查 sys_user 获取 tenantId/userId */
    private final SysUserMapper sysUserMapper;

    /**
     * 登录成功：采集元数据，从 {@link ScaUserDetails} 获取 tenantId/userId/username，发布事件。
     */
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        if (!(authentication.getPrincipal() instanceof ScaUserDetails details)) {
            return;
        }

        RequestMeta meta = collectRequestMeta();

        LoginLogEvent loginLogEvent = new LoginLogEvent();
        loginLogEvent.setTenantId(details.getTenantId());
        loginLogEvent.setUserId(details.getUserId());
        loginLogEvent.setUsername(details.getUsername());
        loginLogEvent.setClientIp(meta.clientIp());
        loginLogEvent.setDevice(meta.device());
        loginLogEvent.setBrowser(meta.browser());
        loginLogEvent.setOs(meta.os());
        loginLogEvent.setIsSuccess(1);
        loginLogEvent.setCostMs(meta.costMs());
        loginLogEvent.setLoginTime(LocalDateTime.now());
        eventPublisher.publishEvent(loginLogEvent);
    }

    /**
     * 登录失败：采集元数据，username 取登录输入的原始值。
     * <p>
     * 失败时 SecurityContext 未建立，拿不到 ScaUserDetails，需通过 username + realm 反查 sys_user
     * 填充 tenantId/userId。用户不存在时（UsernameNotFoundException）反查自然返回 null，留空即可。
     */
    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        Authentication authentication = event.getAuthentication();
        // 仅处理表单登录失败事件，过滤 OAuth2 客户端认证、refresh_token 续期等非用户登录场景
        // （这些事件的 authentication 是 OAuth2ClientAuthenticationToken / OAuth2RefreshTokenAuthenticationToken，其 getName() 返回 client_id，不应记入登录日志）
        if (!(authentication instanceof UsernamePasswordAuthenticationToken)) {
            return;
        }
        String username = authentication.getName();

        RequestMeta meta = collectRequestMeta();

        // SecurityContext 未建立，需反查 sys_user 填充 tenantId/userId
        SysUser user = findUser(username);

        LoginLogEvent loginLogEvent = new LoginLogEvent();
        loginLogEvent.setTenantId(user != null ? user.getTenantId() : null);
        loginLogEvent.setUserId(user != null ? user.getId() : null);
        loginLogEvent.setUsername(username);
        loginLogEvent.setClientIp(meta.clientIp());
        loginLogEvent.setDevice(meta.device());
        loginLogEvent.setBrowser(meta.browser());
        loginLogEvent.setOs(meta.os());
        loginLogEvent.setIsSuccess(0);
        loginLogEvent.setErrorMessage(buildFailureMessage(username, event.getException()));
        loginLogEvent.setCostMs(meta.costMs());
        loginLogEvent.setLoginTime(LocalDateTime.now());
        eventPublisher.publishEvent(loginLogEvent);
    }

    /**
     * 同步采集客户端 IP、设备信息、登录耗时（在请求线程内，ThreadLocal 可用）。
     */
    private RequestMeta collectRequestMeta() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return new RequestMeta(null, null, null, null, null);
        }
        String clientIp = resolveClientIp(request);
        String[] deviceInfo = UserAgentParser.parse(request.getHeader("User-Agent"));
        Long costMs = calculateCostMs(request);
        return new RequestMeta(clientIp, deviceInfo[0], deviceInfo[1], deviceInfo[2], costMs);
    }

    /**
     * 获取当前 HTTP 请求（事件在请求线程内同步发布，RequestContextHolder 可用）。
     */
    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    /**
     * 解析客户端真实 IP，依次尝试常见反向代理请求头，最终回退到 RemoteAddr。
     */
    private String resolveClientIp(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"
        };
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * 计算登录耗时：从请求属性取出 {@link LoginChannelFilter} 记录的开始时间，求差值。
     */
    private Long calculateCostMs(HttpServletRequest request) {
        Object startTime = request.getAttribute(LoginChannelFilter.ATTR_LOGIN_START_TIME);
        if (startTime instanceof Long start) {
            return System.currentTimeMillis() - start;
        }
        return null;
    }

    /**
     * 按 username + realm 反查 sys_user，用于失败时填充 tenantId/userId。
     * <p>
     * realm 与登录渠道一致：admin 渠道 → admin，portal 渠道 → portal。
     * 渠道未设置时默认 admin（与 {@link LoginChannel#fromValue} 的默认行为一致）。
     */
    private SysUser findUser(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        String realm = resolveRealm();
        return sysUserMapper.selectOne(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getRealm, realm)
        );
    }

    /**
     * 从 {@link LoginChannelContext} 获取当前登录渠道并映射为 realm。
     */
    private String resolveRealm() {
        LoginChannel channel = LoginChannelContext.get();
        return channel == LoginChannel.PORTAL ? "portal" : "admin";
    }

    /**
     * 将常见认证异常映射为友好提示，便于审计查阅。
     */
    private String buildFailureMessage(String username, AuthenticationException ex) {
        if (ex instanceof UsernameNotFoundException) {
            return "登录失败：用户 [" + username + "] 不存在";
        }
        if (ex instanceof BadCredentialsException) {
            return "登录失败：用户名或密码错误";
        }
        if (ex instanceof LockedException) {
            return "登录失败：账号已锁定";
        }
        if (ex instanceof DisabledException) {
            return "登录失败：账号已禁用";
        }
        if (ex instanceof AccountExpiredException) {
            return "登录失败：账号已过期";
        }
        if (ex instanceof CredentialsExpiredException) {
            return "登录失败：凭证已过期";
        }
        return "登录失败：" + ex.getMessage();
    }

    /**
     * 请求线程内采集的元数据载体。
     */
    private record RequestMeta(
        String clientIp,
        String device,
        String browser,
        String os,
        Long costMs
    ) {
    }
}
