package io.github.fushuwei.scaskeleton.auth.grant.password;

import io.github.fushuwei.scaskeleton.auth.config.properties.OAuth2ClientProperties;
import io.github.fushuwei.scaskeleton.auth.grant.OAuth2GrantTypeConstants;
import io.github.fushuwei.scaskeleton.auth.grant.base.OAuth2ResourceOwnerBaseAuthenticationProvider;
import io.github.fushuwei.scaskeleton.auth.security.LoginChannel;
import io.github.fushuwei.scaskeleton.auth.security.LoginChannelContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.util.Map;

/**
 * 密码模式认证提供者
 * <p>
 * 负责处理 {@code grant_type=password} 的令牌请求：
 * <ol>
 *   <li>校验客户端是否配置了 password 授权类型</li>
 *   <li>根据 client_id 判定登录渠道（admin / portal）并设置 {@link LoginChannelContext}</li>
 *   <li>从请求参数提取 username / password，委托 {@link AuthenticationManager} 认证</li>
 *   <li>生成并返回 access_token + refresh_token</li>
 * </ol>
 *
 * @author Fu Wei
 */
@Slf4j
public class OAuth2ResourceOwnerPasswordAuthenticationProvider
        extends OAuth2ResourceOwnerBaseAuthenticationProvider<OAuth2ResourceOwnerPasswordAuthenticationToken> {

    /**
     * OAuth2 客户端配置（用于根据 client_id 判定登录渠道）
     */
    private final OAuth2ClientProperties oauth2ClientProperties;

    /**
     * @param authenticationManager    Spring Security 认证管理器
     * @param authorizationService     OAuth2 授权存储服务
     * @param tokenGenerator           OAuth2 令牌生成器
     * @param oauth2ClientProperties   OAuth2 客户端配置
     */
    public OAuth2ResourceOwnerPasswordAuthenticationProvider(
            org.springframework.security.authentication.AuthenticationManager authenticationManager,
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<?> tokenGenerator,
            OAuth2ClientProperties oauth2ClientProperties) {
        super(authenticationManager, authorizationService, tokenGenerator);
        this.oauth2ClientProperties = oauth2ClientProperties;
    }

    @Override
    public UsernamePasswordAuthenticationToken buildToken(Map<String, Object> reqParameters) {
        String username = (String) reqParameters.get("username");
        String password = (String) reqParameters.get("password");
        return new UsernamePasswordAuthenticationToken(username, password);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2ResourceOwnerPasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    public void checkClient(RegisteredClient registeredClient) {
        if (!registeredClient.getAuthorizationGrantTypes().contains(OAuth2GrantTypeConstants.PASSWORD)) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT,
                    "客户端 [" + registeredClient.getClientId() + "] 未配置密码模式授权", null));
        }
    }

    /**
     * 根据 client_id 判定登录渠道：
     * <ul>
     *   <li>{@code sca-portal-client} → {@link LoginChannel#PORTAL}</li>
     *   <li>其它 → {@link LoginChannel#ADMIN}</li>
     * </ul>
     */
    @Override
    public LoginChannel resolveLoginChannel(Map<String, Object> reqParameters) {
        // 附加参数中不包含 client_id（已在 grant_type / scope 过滤时移除），
        // 通过 SecurityContext 中的已认证客户端获取 client_id
        AuthenticationProvider self = this;
        // client_id 不在 additionalParameters 中（被 Converter 过滤为非标准参数保留）
        // 实际上 client_id 会出现在 additionalParameters 中（因为它不是 grant_type 或 scope）
        Object clientId = reqParameters.get("client_id");
        if (clientId != null && oauth2ClientProperties.getPortal().getClientId().equals(clientId)) {
            return LoginChannel.PORTAL;
        }
        return LoginChannel.ADMIN;
    }
}
