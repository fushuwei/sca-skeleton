package io.github.fushuwei.sca.starter.security.user;

import io.github.fushuwei.sca.starter.core.user.CurrentUserProvider;
import io.github.fushuwei.sca.starter.security.context.SecurityUtils;
import io.github.fushuwei.sca.starter.security.properties.SecurityProperties;
import lombok.RequiredArgsConstructor;

/**
 * 基于 Spring Security 资源服务器的当前用户信息提供者。
 * <p>
 * 从 SecurityContext 的 {@link org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication}
 * token 属性（或 JWT 模式下的 Claims）中读取用户 ID 与用户名。
 * <p>
 * 字段名由 {@link SecurityProperties} 的 {@code userIdClaimName}、{@code usernameClaimName} 控制，
 * 默认 {@code sub}、{@code preferred_username}。
 *
 * @author Fu Wei
 */
@RequiredArgsConstructor
public class CurrentUserProviderImpl implements CurrentUserProvider {

    // 注入 Security 配置属性，用于获取自定义的 Claims 字段名配置
    private final SecurityProperties securityProperties;

    /**
     * 从 token 属性中获取当前用户 ID（配置项 userIdClaimName，默认 sub）。
     *
     * @return 用户 ID，未认证时返回 {@code null}
     */
    @Override
    public String getCurrentUserId() {
        return SecurityUtils.getClaim(securityProperties.getUserIdClaimName());
    }

    /**
     * 从 token 属性中获取当前用户名（配置项 usernameClaimName）。
     *
     * @return 用户名，未认证时返回 {@code null}
     */
    @Override
    public String getCurrentUsername() {
        return SecurityUtils.getClaim(securityProperties.getUsernameClaimName());
    }
}
