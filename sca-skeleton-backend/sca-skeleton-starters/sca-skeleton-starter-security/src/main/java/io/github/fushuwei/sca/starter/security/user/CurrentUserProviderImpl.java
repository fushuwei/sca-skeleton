package io.github.fushuwei.sca.starter.security.user;

import io.github.fushuwei.sca.starter.core.user.CurrentUserProvider;
import io.github.fushuwei.sca.starter.security.context.SecurityUtils;
import io.github.fushuwei.sca.starter.security.properties.SecurityProperties;
import lombok.RequiredArgsConstructor;

/**
 * 基于 Spring Security JWT 的当前用户信息提供者。
 * <p>
 * 实现 {@link CurrentUserProvider} 接口，从当前线程 SecurityContext 的 JWT Claims
 * 中提取用户 ID 和用户名，供 MyBatis-Plus 审计字段填充、操作日志、限流等通用能力使用。
 * <p>
 * 具体提取哪个 Claims 字段由 {@link SecurityProperties} 的
 * {@code userIdClaimName} 和 {@code usernameClaimName} 控制，
 * 默认对应 JWT 标准字段 {@code sub} 和 {@code preferred_username}。
 *
 * @author Fu Wei
 */
@RequiredArgsConstructor
public class CurrentUserProviderImpl implements CurrentUserProvider {

    // 注入 Security 配置属性，用于获取自定义的 Claims 字段名配置
    private final SecurityProperties securityProperties;

    /**
     * 从 JWT Claims 中获取当前用户 ID。
     * 使用 {@link SecurityProperties#getUserIdClaimName()} 指定的 Claims 字段。
     *
     * @return 用户 ID，未认证时返回 {@code null}
     */
    @Override
    public String getCurrentUserId() {
        // 从 JWT 中读取用户 ID 字段（默认为 sub）
        return SecurityUtils.getClaim(securityProperties.getUserIdClaimName());
    }

    /**
     * 从 JWT Claims 中获取当前用户名。
     * 使用 {@link SecurityProperties#getUsernameClaimName()} 指定的 Claims 字段。
     *
     * @return 用户名，未认证时返回 {@code null}
     */
    @Override
    public String getCurrentUsername() {
        // 从 JWT 中读取用户名字段（默认为 preferred_username）
        return SecurityUtils.getClaim(securityProperties.getUsernameClaimName());
    }
}
