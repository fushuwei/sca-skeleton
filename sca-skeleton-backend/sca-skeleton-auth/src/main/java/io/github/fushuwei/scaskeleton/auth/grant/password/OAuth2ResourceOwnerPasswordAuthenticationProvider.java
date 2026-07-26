package io.github.fushuwei.scaskeleton.auth.grant.password;

import io.github.fushuwei.scaskeleton.auth.config.properties.OAuth2ClientProperties;
import io.github.fushuwei.scaskeleton.auth.grant.OAuth2GrantTypeConstants;
import io.github.fushuwei.scaskeleton.auth.grant.base.OAuth2ResourceOwnerBaseAuthenticationProvider;
import io.github.fushuwei.scaskeleton.auth.security.LoginChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
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
 *   <li>根据 client_id 判定登录渠道（admin / portal）并设置 LoginChannelContext</li>
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
     * @param eventPublisher           Spring 事件发布器
     * @param oauth2ClientProperties   OAuth2 客户端配置
     */
    public OAuth2ResourceOwnerPasswordAuthenticationProvider(
            AuthenticationManager authenticationManager,
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<?> tokenGenerator,
            ApplicationEventPublisher eventPublisher,
            OAuth2ClientProperties oauth2ClientProperties) {
        super(authenticationManager, authorizationService, tokenGenerator, eventPublisher);
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
     * 根据已认证客户端的 client_id 判定登录渠道：
     * <ul>
     *   <li>{@code sca-portal-client} → {@link LoginChannel#PORTAL}</li>
     *   <li>其它 → {@link LoginChannel#ADMIN}</li>
     * </ul>
     * <p>
     * 使用 {@code registeredClient.getClientId()} 而非请求参数中的 client_id，
     * 因为机密客户端使用 {@code client_secret_basic} 认证方式时，
     * client_id 在 Authorization 头中，不在请求体参数中。
     */
    @Override
    public LoginChannel resolveLoginChannel(RegisteredClient registeredClient) {
        if (oauth2ClientProperties.getPortal().getClientId().equals(registeredClient.getClientId())) {
            return LoginChannel.PORTAL;
        }
        return LoginChannel.ADMIN;
    }
}
