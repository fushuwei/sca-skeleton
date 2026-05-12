package io.github.fushuwei.sca.auth.extension.password;

import io.github.fushuwei.sca.auth.security.ScaUserDetails;
import io.github.fushuwei.sca.auth.security.ScaUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.security.Principal;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 自定义密码授权模式认证提供者。
 * <p>
 * 流程：
 * <ol>
 *   <li>提取并验证已认证的客户端主体（由 SAS 客户端认证过滤器完成）</li>
 *   <li>通过 {@link ScaUserDetailsService} 加载用户，验证密码与账号状态</li>
 *   <li>计算最终允许的 scope（取请求 scope 与客户端注册 scope 的交集）</li>
 *   <li>调用 SAS 标准 {@link OAuth2TokenGenerator} 生成 access_token 和 refresh_token</li>
 *   <li>将授权记录持久化到 {@link OAuth2AuthorizationService}（Redis 存储）</li>
 * </ol>
 *
 * @author Fu Wei
 */
@Slf4j
@RequiredArgsConstructor
public class PasswordGrantAuthenticationProvider implements AuthenticationProvider {

    private final ScaUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        PasswordGrantAuthenticationToken passwordToken =
                (PasswordGrantAuthenticationToken) authentication;

        // 1. 获取已认证的客户端（SAS 标准客户端认证过滤器已完成客户端验证）
        OAuth2ClientAuthenticationToken clientPrincipal =
                getAuthenticatedClientElseThrowInvalidClient(passwordToken);
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();

        // 2. 验证客户端是否支持 password 授权类型
        if (registeredClient == null || !registeredClient.getAuthorizationGrantTypes()
                .contains(PasswordGrantAuthenticationToken.PASSWORD)) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
        }

        // 3. 加载并验证用户
        UserDetails userDetails;
        try {
            if (passwordToken.getTenantId() != null) {
                userDetails = userDetailsService.loadUserByUsernameAndTenant(
                        passwordToken.getUsername(), passwordToken.getTenantId());
            } else {
                userDetails = userDetailsService.loadUserByUsername(passwordToken.getUsername());
            }
        } catch (AuthenticationException ex) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, ex.getMessage(), null), ex);
        }

        // 4. 校验账号状态
        checkUserStatus(userDetails);

        // 5. 校验密码
        if (!passwordEncoder.matches(passwordToken.getPassword(), userDetails.getPassword())) {
            log.warn("用户 [{}] 密码验证失败", passwordToken.getUsername());
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "用户名或密码错误", null));
        }

        // 6. 计算最终 scope（请求 scope ∩ 客户端注册 scope，空请求则使用注册 scope）
        Set<String> authorizedScopes;
        Set<String> requestedScopes = passwordToken.getScopes();
        if (requestedScopes == null || requestedScopes.isEmpty()) {
            authorizedScopes = new LinkedHashSet<>(registeredClient.getScopes());
        } else {
            Set<String> allowedScopes = registeredClient.getScopes();
            for (String scope : requestedScopes) {
                if (!allowedScopes.contains(scope)) {
                    throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_SCOPE);
                }
            }
            authorizedScopes = new LinkedHashSet<>(requestedScopes);
        }

        // 7. 构建用户认证对象（用于 Token 上下文，TokenCustomizer 从中读取 Principal）
        UsernamePasswordAuthenticationToken userAuthentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        userDetails, null, userDetails.getAuthorities());

        // 8. 构建 Token 上下文
        DefaultOAuth2TokenContext.Builder tokenContextBuilder = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(userAuthentication)
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .authorizedScopes(authorizedScopes)
                .authorizationGrantType(PasswordGrantAuthenticationToken.PASSWORD)
                .authorizationGrant(passwordToken);

        // 9. 生成 access_token
        DefaultOAuth2TokenContext accessTokenContext = tokenContextBuilder
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .build();
        OAuth2Token generatedAccessToken = tokenGenerator.generate(accessTokenContext);
        if (generatedAccessToken == null) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.SERVER_ERROR);
        }
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                generatedAccessToken.getTokenValue(),
                generatedAccessToken.getIssuedAt(),
                generatedAccessToken.getExpiresAt(),
                authorizedScopes);

        // 10. 构建授权记录（先不含 refresh_token）
        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization
                .withRegisteredClient(registeredClient)
                .principalName(userDetails.getUsername())
                .authorizationGrantType(PasswordGrantAuthenticationToken.PASSWORD)
                .authorizedScopes(authorizedScopes)
                .attribute(Principal.class.getName(), userAuthentication);

        if (generatedAccessToken instanceof ClaimAccessor claimAccessor) {
            authorizationBuilder
                    .token(accessToken, metadata -> metadata.put(
                            OAuth2Authorization.Token.CLAIMS_METADATA_NAME,
                            claimAccessor.getClaims()));
        } else {
            authorizationBuilder.accessToken(accessToken);
        }

        // 11. 生成 refresh_token（如果客户端支持）
        OAuth2RefreshToken refreshToken = null;
        if (registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)
                && !clientPrincipal.getClientAuthenticationMethod()
                .equals(ClientAuthenticationMethod.NONE)) {
            DefaultOAuth2TokenContext refreshTokenContext = tokenContextBuilder
                    .tokenType(OAuth2TokenType.REFRESH_TOKEN)
                    .build();
            OAuth2Token generatedRefreshToken = tokenGenerator.generate(refreshTokenContext);
            if (generatedRefreshToken instanceof OAuth2RefreshToken rt) {
                refreshToken = rt;
                authorizationBuilder.refreshToken(refreshToken);
            }
        }

        // 12. 持久化授权记录
        OAuth2Authorization authorization = authorizationBuilder.build();
        authorizationService.save(authorization);

        log.info("用户 [{}] 登录成功，客户端 [{}]",
                userDetails.getUsername(), registeredClient.getClientId());

        return new OAuth2AccessTokenAuthenticationToken(
                registeredClient, clientPrincipal, accessToken, refreshToken,
                ((ScaUserDetails) userDetails).getPermissions().isEmpty()
                        ? java.util.Collections.emptyMap()
                        : java.util.Map.of());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return PasswordGrantAuthenticationToken.class.isAssignableFrom(authentication);
    }

    /** 提取已认证客户端；未认证则抛出 invalid_client 异常。 */
    private OAuth2ClientAuthenticationToken getAuthenticatedClientElseThrowInvalidClient(
            Authentication authentication) {
        OAuth2ClientAuthenticationToken clientPrincipal = null;
        if (OAuth2ClientAuthenticationToken.class.isAssignableFrom(
                authentication.getPrincipal().getClass())) {
            clientPrincipal = (OAuth2ClientAuthenticationToken) authentication.getPrincipal();
        }
        if (clientPrincipal != null && clientPrincipal.isAuthenticated()) {
            return clientPrincipal;
        }
        throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
    }

    /** 校验账号状态，抛出对应 Spring Security 异常（SAS 会将其转为标准 OAuth2 错误响应）。 */
    private void checkUserStatus(UserDetails userDetails) {
        if (!userDetails.isEnabled()) {
            throw new DisabledException("账号已禁用");
        }
        if (!userDetails.isAccountNonLocked()) {
            throw new LockedException("账号已锁定");
        }
        if (!userDetails.isAccountNonExpired()) {
            throw new AccountExpiredException("账号已过期");
        }
        if (!userDetails.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException("密码已过期，请联系管理员重置");
        }
    }
}
