package io.github.fushuwei.sca.auth.service;

import io.github.fushuwei.sca.auth.config.ScaAuthProperties;
import io.github.fushuwei.sca.auth.infrastructure.entity.SysUser;
import io.github.fushuwei.sca.auth.infrastructure.mapper.PermissionMapper;
import io.github.fushuwei.sca.auth.infrastructure.mapper.SysUserMapper;
import io.github.fushuwei.sca.auth.security.ScaUserDetails;
import io.github.fushuwei.sca.starter.core.exception.BusinessException;
import io.github.fushuwei.sca.starter.core.exception.ErrorCode;
import io.github.fushuwei.sca.starter.core.id.UuidUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 登录与刷新令牌签发，签发协议对齐 OAuth2 JWT profile。
 *
 * @author Fu Wei
 */
@Service
@RequiredArgsConstructor
public class AuthTokenService {

    // 认证管理器，完成密码校验。
    private final AuthenticationManager authenticationManager;
    // JWT 编码器。
    private final JwtEncoder jwtEncoder;
    // 配置项。
    private final ScaAuthProperties authProperties;
    // Redis 存刷新令牌。
    private final StringRedisTemplate stringRedisTemplate;
    // 用户表。
    private final SysUserMapper sysUserMapper;
    // 权限查询。
    private final PermissionMapper permissionMapper;
    // 刷新令牌前缀。
    private static final String REFRESH_PREFIX = "auth:refresh:";

    // 用户名密码登录，返回访问令牌与刷新令牌。
    public TokenPair login(String tenantId, String username, String password) {
        // 组装与 UserDetailsService 一致的 principal。
        String principal = io.github.fushuwei.sca.auth.security.ScaUserDetailsService.composePrincipal(tenantId, username);
        // 构造用户名密码令牌。
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(principal, password);
        // 执行认证。
        Authentication authentication = authenticationManager.authenticate(token);
        // 取回用户主体。
        ScaUserDetails details = (ScaUserDetails) authentication.getPrincipal();
        // 签发令牌对。
        return issueTokenPair(details);
    }

    // 刷新访问令牌。
    public TokenPair refresh(String refreshToken) {
        // 读取用户 ID。
        String cacheKey = REFRESH_PREFIX + refreshToken;
        String userId = stringRedisTemplate.opsForValue().get(cacheKey);
        // 不存在则拒绝。
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "refresh token invalid");
        }
        // 单次刷新即作废旧令牌，降低重放风险。
        stringRedisTemplate.delete(cacheKey);
        // 查询用户。
        SysUser user = sysUserMapper.selectById(userId);
        // 用户不存在则拒绝。
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "user not found");
        }
        // 重新加载权限。
        List<String> codes = permissionMapper.selectPermissionCodesByUserId(user.getId());
        // 判断是否仍可用。
        boolean active = "active".equalsIgnoreCase(user.getStatus());
        // 组装主体。
        ScaUserDetails details = new ScaUserDetails(
                user.getId(),
                user.getTenantId(),
                user.getUsername(),
                user.getPassword(),
                active,
                codes);
        // 签发新对。
        return issueTokenPair(details);
    }

    // 基于主体生成访问与刷新令牌。
    private TokenPair issueTokenPair(ScaUserDetails details) {
        // 非激活用户拒绝。
        if (!details.isEnabled()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "user disabled");
        }
        // 当前时间。
        Instant now = Instant.now();
        // 过期时间。
        Instant accessExpiry = now.plusSeconds(authProperties.getAccessTokenTtlSeconds());
        // scope 字符串。
        String scope = details.getAuthorities().stream()
                .map(a -> a.getAuthority().replaceFirst("^SCOPE_", ""))
                .collect(Collectors.joining(" "));
        // 组装声明。
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(authProperties.getIssuer())
                .subject(details.getUsername())
                .issuedAt(now)
                .expiresAt(accessExpiry)
                .claim("tenant_id", details.getTenantId())
                .claim("user_id", details.getUserId())
                .claim("scope", scope)
                .build();
        // 使用 RS256 头。
        org.springframework.security.oauth2.jwt.JwsHeader jwsHeader =
                org.springframework.security.oauth2.jwt.JwsHeader.with(() -> "RS256").build();
        // 编码 JWT。
        Jwt accessJwt = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims));
        // 生成刷新随机串。
        String refresh = UuidUtils.nextSimpleStr();
        // 写入 Redis。
        stringRedisTemplate.opsForValue()
                .set(REFRESH_PREFIX + refresh, details.getUserId(), Duration.ofSeconds(authProperties.getRefreshTokenTtlSeconds()));
        // 返回对。
        return new TokenPair(accessJwt.getTokenValue(), refresh, authProperties.getAccessTokenTtlSeconds());
    }

    /**
     * 令牌对响应模型。
     *
     * @param accessToken 访问令牌
     * @param refreshToken 刷新令牌
     * @param expiresIn 过期秒
     */
    public record TokenPair(String accessToken, String refreshToken, long expiresIn) {
    }
}
