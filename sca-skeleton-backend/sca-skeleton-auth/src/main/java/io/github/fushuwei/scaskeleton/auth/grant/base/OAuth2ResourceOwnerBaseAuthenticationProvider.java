package io.github.fushuwei.scaskeleton.auth.grant.base;

import io.github.fushuwei.scaskeleton.auth.security.LoginChannel;
import io.github.fushuwei.scaskeleton.auth.security.LoginChannelContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.Principal;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 自定义授权模式基础认证提供者
 * <p>
 * 核心职责：
 * <ol>
 *   <li>校验客户端是否支持当前授权模式</li>
 *   <li>调用 {@link AuthenticationManager} 完成用户认证</li>
 *   <li>设置 {@link LoginChannelContext} 以驱动 {@code RoutingUserDetailsService} 的渠道路由</li>
 *   <li>生成 access_token 和 refresh_token</li>
 *   <li>构建并持久化 {@link OAuth2Authorization}</li>
 *   <li>将 Spring Security 认证异常映射为标准 OAuth2 错误响应</li>
 * </ol>
 * <p>
 * 相比 pig 项目的改进：
 * <ul>
 *   <li>在 Provider 内部统一管理 {@code LoginChannelContext} 的设置与清理，无需额外 Filter</li>
 *   <li>登录开始时间由 Provider 写入请求属性，移除对 LoginChannelFilter 的依赖</li>
 *   <li>不依赖 hutool {@code SpringUtil}，使用构造器注入</li>
 *   <li>异常映射使用标准 OAuth2 错误码，不引入自定义错误码扩展</li>
 *   <li>认证成功/失败事件由 Provider 直接发布（携带原始 UsernamePasswordAuthenticationToken），
 *       确保 LoginLogPublisher 和 LoginAttemptEventListener 能正确匹配事件类型</li>
 * </ul>
 *
 * @param <T> 子模式对应的 AuthenticationToken 类型
 * @author Fu Wei
 */
@Slf4j
public abstract class OAuth2ResourceOwnerBaseAuthenticationProvider
        <T extends OAuth2ResourceOwnerBaseAuthenticationToken>
        implements org.springframework.security.authentication.AuthenticationProvider {

    private static final String ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2";

    /**
     * 请求属性名：登录开始时间（毫秒），供 LoginLogPublisher 计算认证耗时
     */
    public static final String ATTR_LOGIN_START_TIME = "loginStartTime";

    /**
     * 请求属性名：登录渠道（admin/portal），供 LoginLogPublisher 确定用户 realm
     */
    public static final String ATTR_LOGIN_CHANNEL = "loginChannel";

    private final AuthenticationManager authenticationManager;
    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * @param authenticationManager Spring Security 认证管理器（配置了 DaoAuthenticationProvider）
     * @param authorizationService  OAuth2 授权存储服务（Redis + JDBC）
     * @param tokenGenerator        OAuth2 令牌生成器（不透明 access_token + refresh_token）
     * @param eventPublisher        Spring 事件发布器（发布登录成功/失败事件，驱动登录日志与账号锁定）
     */
    protected OAuth2ResourceOwnerBaseAuthenticationProvider(AuthenticationManager authenticationManager,
                                                             OAuth2AuthorizationService authorizationService,
                                                             OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
                                                             ApplicationEventPublisher eventPublisher) {
        Assert.notNull(authenticationManager, "authenticationManager cannot be null");
        Assert.notNull(authorizationService, "authorizationService cannot be null");
        Assert.notNull(tokenGenerator, "tokenGenerator cannot be null");
        Assert.notNull(eventPublisher, "eventPublisher cannot be null");
        this.authenticationManager = authenticationManager;
        this.authorizationService = authorizationService;
        this.tokenGenerator = tokenGenerator;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 子类从此方法的参数中提取用户名、密码等字段，构建 {@link UsernamePasswordAuthenticationToken}。
     *
     * @param reqParameters 附加参数（username、password 等）
     * @return 未认证的 UsernamePasswordAuthenticationToken
     */
    public abstract UsernamePasswordAuthenticationToken buildToken(Map<String, Object> reqParameters);

    /**
     * 判断当前 Provider 是否支持指定类型的 AuthenticationToken。
     */
    @Override
    public abstract boolean supports(Class<?> authentication);

    /**
     * 校验已注册客户端是否允许使用当前授权模式。
     * <p>
     * 子类应检查 {@code registeredClient.getAuthorizationGrantTypes()} 是否包含当前授权类型，
     * 不支持时抛出 {@link OAuth2AuthenticationException}。
     *
     * @param registeredClient 已注册的客户端
     */
    public abstract void checkClient(RegisteredClient registeredClient);

    /**
     * 从已认证的客户端解析登录渠道（admin / portal），用于设置 {@link LoginChannelContext}。
     * <p>
     * 默认实现返回 {@link LoginChannel#ADMIN}。子类应根据 {@code registeredClient.getClientId()}
     * 判定渠道，而非从请求参数中读取 client_id（机密客户端使用 {@code client_secret_basic} 时
     * client_id 在 Authorization 头中，不在请求体参数中）。
     *
     * @param registeredClient 已认证的客户端
     * @return 登录渠道
     */
    @SuppressWarnings("unused")
    public LoginChannel resolveLoginChannel(RegisteredClient registeredClient) {
        return LoginChannel.ADMIN;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        @SuppressWarnings("unchecked")
        T resourceOwnerAuthentication = (T) authentication;

        // 1) 校验客户端认证状态
        OAuth2ClientAuthenticationToken clientPrincipal = getAuthenticatedClientElseThrowInvalidClient(
                resourceOwnerAuthentication);
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();

        // 2) 校验客户端是否支持当前授权模式
        checkClient(registeredClient);

        // 3) 校验并确定授权范围
        Set<String> authorizedScopes = resolveAuthorizedScopes(resourceOwnerAuthentication, registeredClient);

        // 4) 从已认证的客户端解析登录渠道并写入 ThreadLocal
        Map<String, Object> reqParameters = resourceOwnerAuthentication.getAdditionalParameters();
        LoginChannel channel = resolveLoginChannel(registeredClient);
        LoginChannelContext.set(channel);

        // 5) 记录登录开始时间与渠道到请求属性，供 LoginLogPublisher 使用
        recordLoginStartTime(channel);

        log.debug("密码模式认证开始：client_id={}, channel={}, username={}",
                registeredClient.getClientId(), channel.getValue(),
                reqParameters.get("username"));

        // 6) 构建 UsernamePasswordAuthenticationToken 并委托 AuthenticationManager 认证
        UsernamePasswordAuthenticationToken usernamePasswordToken = buildToken(reqParameters);

        try {
            Authentication usernamePasswordAuthentication = authenticationManager.authenticate(usernamePasswordToken);

            log.info("密码模式认证成功：client_id={}, username={}",
                    registeredClient.getClientId(), usernamePasswordAuthentication.getName());

            // 7) 直接发布认证成功事件（携带 ScaUserDetails），驱动登录日志与失败计数清零。
            //    在 finally 清理 LoginChannelContext 之前发布，确保监听器能读取渠道。
            eventPublisher.publishEvent(new AuthenticationSuccessEvent(usernamePasswordAuthentication));

            // 8) 生成令牌并构建 OAuth2Authorization
            return generateAccessToken(resourceOwnerAuthentication, clientPrincipal, registeredClient,
                    authorizedScopes, usernamePasswordAuthentication);

        } catch (AuthenticationException ex) {
            log.warn("密码模式认证失败：client_id={}, username={}, error={}",
                    registeredClient.getClientId(), reqParameters.get("username"), ex.getMessage());

            // 直接发布认证失败事件（携带原始 UsernamePasswordAuthenticationToken），驱动登录日志与失败计数。
            // 在 finally 清理 LoginChannelContext 之前发布，确保监听器能读取渠道。
            eventPublisher.publishEvent(new AuthenticationFailureBadCredentialsEvent(usernamePasswordToken, ex));

            throw mapToOAuth2AuthenticationException(authentication, ex);
        } catch (Exception e) {
            log.error("密码模式认证异常：client_id={}, username={}", registeredClient.getClientId(),
                    reqParameters.get("username"), e);
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
                    "认证服务内部错误", ERROR_URI));
        } finally {
            // 清理 ThreadLocal，防止线程池复用导致渠道串扰
            LoginChannelContext.clear();
        }
    }

    /**
     * 校验请求的 scope 是否在客户端配置的 scope 范围内。
     */
    private Set<String> resolveAuthorizedScopes(T resourceOwnerAuthentication, RegisteredClient registeredClient) {
        Set<String> authorizedScopes;
        if (!CollectionUtils.isEmpty(resourceOwnerAuthentication.getScopes())) {
            for (String requestedScope : resourceOwnerAuthentication.getScopes()) {
                if (!registeredClient.getScopes().contains(requestedScope)) {
                    throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_SCOPE);
                }
            }
            authorizedScopes = new LinkedHashSet<>(resourceOwnerAuthentication.getScopes());
        } else {
            authorizedScopes = new LinkedHashSet<>();
        }
        return authorizedScopes;
    }

    /**
     * 生成 access_token、refresh_token，构建并持久化 OAuth2Authorization。
     */
    private OAuth2AccessTokenAuthenticationToken generateAccessToken(
            T resourceOwnerAuthentication,
            OAuth2ClientAuthenticationToken clientPrincipal,
            RegisteredClient registeredClient,
            Set<String> authorizedScopes,
            Authentication usernamePasswordAuthentication) {

        // 构建 Token 上下文
        DefaultOAuth2TokenContext.Builder tokenContextBuilder = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(usernamePasswordAuthentication)
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .authorizedScopes(authorizedScopes)
                .authorizationGrantType(resourceOwnerAuthentication.getAuthorizationGrantType())
                .authorizationGrant(resourceOwnerAuthentication);

        // 构建 Authorization Builder
        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName(usernamePasswordAuthentication.getName())
                .authorizationGrantType(resourceOwnerAuthentication.getAuthorizationGrantType())
                .authorizedScopes(authorizedScopes);

        // ── 生成 access_token ──
        DefaultOAuth2TokenContext tokenContext = tokenContextBuilder
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .build();
        OAuth2Token generatedAccessToken = tokenGenerator.generate(tokenContext);
        if (generatedAccessToken == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
                    "令牌生成器未能生成 access_token", ERROR_URI));
        }
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                generatedAccessToken.getTokenValue(),
                generatedAccessToken.getIssuedAt(),
                generatedAccessToken.getExpiresAt(),
                tokenContext.getAuthorizedScopes());

        // 不透明令牌携带 claims 元数据
        if (generatedAccessToken instanceof ClaimAccessor claimAccessor) {
            authorizationBuilder.id(accessToken.getTokenValue())
                    .token(accessToken, metadata -> metadata.put(
                            OAuth2Authorization.Token.CLAIMS_METADATA_NAME,
                            claimAccessor.getClaims()))
                    .authorizedScopes(authorizedScopes)
                    .attribute(Principal.class.getName(), usernamePasswordAuthentication);
        } else {
            authorizationBuilder.id(accessToken.getTokenValue()).accessToken(accessToken);
        }

        // ── 生成 refresh_token ──
        OAuth2RefreshToken refreshToken = generateRefreshToken(tokenContextBuilder, registeredClient);
        if (refreshToken != null) {
            authorizationBuilder.refreshToken(refreshToken);
        }

        // ── 持久化授权记录 ──
        OAuth2Authorization authorization = authorizationBuilder.build();
        authorizationService.save(authorization);

        log.debug("令牌生成完成：client_id={}, access_token={}..., refresh_token={}...",
                registeredClient.getClientId(),
                maskToken(accessToken.getTokenValue()),
                refreshToken != null ? maskToken(refreshToken.getTokenValue()) : "null");

        // 返回包含令牌信息的认证响应
        return new OAuth2AccessTokenAuthenticationToken(
                registeredClient, clientPrincipal, accessToken, refreshToken,
                Objects.requireNonNullElse(authorization.getAccessToken().getClaims(), java.util.Collections.emptyMap()));
    }

    /**
     * 生成 refresh_token（仅当客户端配置了 REFRESH_TOKEN 授权类型时）。
     */
    private OAuth2RefreshToken generateRefreshToken(DefaultOAuth2TokenContext.Builder tokenContextBuilder,
                                                     RegisteredClient registeredClient) {
        if (!registeredClient.getAuthorizationGrantTypes()
                .contains(org.springframework.security.oauth2.core.AuthorizationGrantType.REFRESH_TOKEN)) {
            return null;
        }
        DefaultOAuth2TokenContext refreshTokenContext = tokenContextBuilder
                .tokenType(OAuth2TokenType.REFRESH_TOKEN)
                .build();
        OAuth2Token generatedRefreshToken = tokenGenerator.generate(refreshTokenContext);
        if (!(generatedRefreshToken instanceof OAuth2RefreshToken refreshToken)) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
                    "令牌生成器未能生成 refresh_token", ERROR_URI));
        }
        return refreshToken;
    }

    /**
     * 从认证主体中提取已认证的客户端，未认证时抛出 invalid_client 错误。
     */
    private OAuth2ClientAuthenticationToken getAuthenticatedClientElseThrowInvalidClient(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2ClientAuthenticationToken clientPrincipal
                && clientPrincipal.isAuthenticated()) {
            return clientPrincipal;
        }
        throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
    }

    /**
     * 将 Spring Security 认证异常映射为标准 OAuth2 错误响应。
     * <p>
     * {@code UsernameNotFoundException} 与 {@code BadCredentialsException} 统一返回
     * "用户名或密码错误"，防止用户名枚举攻击。
     * （{@code DaoAuthenticationProvider} 默认 {@code hideUserNotFoundExceptions=true}
     * 已将 {@code UsernameNotFoundException} 转换为 {@code BadCredentialsException}，
     * 此处做双重防护，确保即使修改默认配置也不暴露用户是否存在。）
     */
    private OAuth2AuthenticationException mapToOAuth2AuthenticationException(
            Authentication authentication, AuthenticationException ex) {
        if (ex instanceof OAuth2AuthenticationException oauth2Ex) {
            return oauth2Ex;
        }
        if (ex instanceof UsernameNotFoundException || ex instanceof BadCredentialsException) {
            return new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST,
                    "用户名或密码错误", ERROR_URI));
        }
        if (ex instanceof LockedException) {
            return new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST,
                    "账号已锁定", ERROR_URI));
        }
        if (ex instanceof DisabledException) {
            return new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST,
                    "账号已禁用", ERROR_URI));
        }
        if (ex instanceof AccountExpiredException) {
            return new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST,
                    "账号已过期", ERROR_URI));
        }
        if (ex instanceof CredentialsExpiredException) {
            return new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST,
                    "凭证已过期", ERROR_URI));
        }
        log.error("未预期的认证异常", ex);
        return new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
                ex.getMessage(), ERROR_URI));
    }

    /**
     * 将登录开始时间和渠道写入请求属性，供 LoginLogPublisher 使用。
     */
    private void recordLoginStartTime(LoginChannel channel) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            request.setAttribute(ATTR_LOGIN_START_TIME, System.currentTimeMillis());
            request.setAttribute(ATTR_LOGIN_CHANNEL, channel.getValue());
        }
    }

    /**
     * 令牌脱敏（仅展示前 8 位），用于日志输出。
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "****";
        }
        return token.substring(0, 8) + "****";
    }
}
