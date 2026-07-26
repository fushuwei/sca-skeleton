package io.github.fushuwei.scaskeleton.auth.grant.base;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 自定义授权模式基础认证令牌
 * <p>
 * 所有自定义授权模式（密码模式、短信模式等）的 AuthenticationToken 均继承本类，
 * 统一持有 {@code authorizationGrantType}、{@code clientPrincipal}、{@code scopes}
 * 与 {@code additionalParameters} 四个核心字段，避免各子类重复实现。
 * <p>
 * 设计参考 pig 项目的 {@code OAuth2ResourceOwnerBaseAuthenticationToken}，
 * 去除对 pig 内部工具类的依赖，使用标准 Spring Security API。
 *
 * @author Fu Wei
 */
@Getter
public abstract class OAuth2ResourceOwnerBaseAuthenticationToken extends AbstractAuthenticationToken {

    /**
     * 授权类型（如 password、sms 等）
     */
    private final AuthorizationGrantType authorizationGrantType;

    /**
     * 已认证的客户端主体（由 SAS 的客户端认证过滤器写入 SecurityContext）
     */
    private final Authentication clientPrincipal;

    /**
     * 请求的权限范围
     */
    private final Set<String> scopes;

    /**
     * 附加参数（username、password、sms_code 等，具体由子模式定义）
     */
    private final Map<String, Object> additionalParameters;

    /**
     * 构造未认证的基础令牌（初始状态，principal 尚未认证）。
     *
     * @param authorizationGrantType 授权类型
     * @param clientPrincipal        已认证的客户端主体
     * @param scopes                 请求的权限范围（可为 null）
     * @param additionalParameters   附加参数（可为 null）
     */
    protected OAuth2ResourceOwnerBaseAuthenticationToken(AuthorizationGrantType authorizationGrantType,
                                                         Authentication clientPrincipal,
                                                         @Nullable Set<String> scopes,
                                                         @Nullable Map<String, Object> additionalParameters) {
        super(Collections.emptyList());
        Assert.notNull(authorizationGrantType, "authorizationGrantType cannot be null");
        Assert.notNull(clientPrincipal, "clientPrincipal cannot be null");
        this.authorizationGrantType = authorizationGrantType;
        this.clientPrincipal = clientPrincipal;
        this.scopes = Collections.unmodifiableSet(scopes != null ? new HashSet<>(scopes) : Collections.emptySet());
        this.additionalParameters = Collections.unmodifiableMap(
                additionalParameters != null ? new HashMap<>(additionalParameters) : Collections.emptyMap());
    }

    /**
     * 自定义授权模式通常不直接持有用户凭证，credentials 返回空字符串。
     * 用户认证由 {@code AuthenticationManager} 在 Provider 内部完成。
     */
    @Override
    public Object getCredentials() {
        return "";
    }

    /**
     * 返回已认证的客户端主体作为 principal。
     */
    @Override
    public Object getPrincipal() {
        return this.clientPrincipal;
    }
}
