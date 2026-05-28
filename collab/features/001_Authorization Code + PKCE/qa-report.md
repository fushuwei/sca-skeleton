# QA Report — OAuth 2.1 Authorization Code + PKCE

> **审查日期**: 2026-05-28
> **审查范围**: 后端 Auth 服务、Gateway、Starter-Security、前端 OAuth 共享库、Admin SPA
> **审查方法**: 代码静态分析

---

## 总体评价

整体实现质量较高，架构清晰（Gateway 统一入口 → Auth Server 发牌 → Resource Server Redis 自省），PKCE 流程完整，前后端配合到位。以下按严重程度列出发现的问题与改进建议。

---

## P0 — 阻断级

### P0-1. Token 吊销端点未放行网关白名单

- **文件**: `sca-skeleton-gateway-dev.yaml:140-148`
- **问题**: 前端 `revokeOAuthToken()` 请求 `/auth/oauth2/revoke`，但网关 whitelist 只放了 `authorize`、`token`、`jwks`，未包含 `revoke`。若令牌已过期，携带过期 token 调用将被网关 401 拦截，导致吊销失败（logout 时 token 可能仍有效，但刷新令牌吊销则必然失败）。
- **风险**: 高。登出时远端令牌残留，无法真正吊销。
- **建议**: whitelist 新增 `/auth/oauth2/revoke`。

---

## P1 — 严重

### P1-1. 验证码（Captcha）能力未接入认证流程

- **文件**: `sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/config/AuthSecurityConfig.java` (第 60-88 行)
- **问题**: `sca-skeleton-starter-captcha` 模块提供了完整的 `CaptchaService` 和 Redis 存储/校验能力，但 Auth 服务的表单认证链中**没有任何 Filter 或 Provider 调用 `CaptchaService.verify()`**。登录模板中也没有验证码输入框。验证码能力形同虚设。
- **风险**: 高。无验证码防护 → 暴力破解/撞库/凭证填充攻击。
- **建议**:
  1. 在 `AuthSecurityConfig` 表单链中 `UsernamePasswordAuthenticationFilter` **之前**插入 Captcha 校验 Filter。
  2. 登录模板增加验证码输入 `input` 和服务端生成的 `captchaKey`。
  3. Gateway whitelist 保留 `/auth/captcha/**`。

### P1-2. 无 DPoP / mTLS 令牌绑定（Sender Constraint）

- **文件**: `RegisteredClientInitializer.java:84-88`
- **问题**: 当前使用纯 `Bearer` 令牌，不绑定客户端。令牌一旦泄露，攻击者可在任意设备上使用。OAuth 2.1 BCP (RFC 9700) 推荐 DPoP 或 mTLS 作为 sender-constrained token。
- **风险**: 中高。取决于令牌泄露可能性（浏览器 SessionStorage、网络传输、日志等）。
- **建议**: 规划 DPoP 支持，至少在不透明令牌中增加 `cnf` (confirmation) claim。

### P1-3. `/oauth2/authorize` 端点无速率限制

- **文件**: `sca-skeleton-gateway-dev.yaml:34-45`
- **问题**: Gateway 仅对 `/auth/oauth2/token` 做了 RequestRateLimiter (10/20)，但 `/auth/oauth2/authorize` 无速率限制。攻击者可批量请求 authorize（即使不完成登录），消耗 Auth 服务 Redis 存储（每次生成 authorization_code 写入 Redis）。
- **风险**: 中高。Redis 资源耗尽或放大反射攻击。
- **建议**: Gateway 对 `/auth/oauth2/authorize` 补充按 IP 的速率限制；Auth 侧也限制同一 `client_id` 的未完成 authorize 请求数。

---

## P2 — 中等

### P2-1. Redis 二级索引 `set` + `expire` 非原子操作

- **文件**: `RedisOAuth2AuthorizationService.java:442-448`
- **问题**: `setIndex()` 先执行 `SET key value`，再执行 `EXPIRE key ttl`。若两步之间 Redis 崩溃或主从切换，索引键将**永不过期**，造成内存泄漏。
- **风险**: 中。累积效应可能导致 Redis 内存耗尽。
- **建议**: 改用 `SET key value EX ttl NX` 单命令，或使用 `stringRedisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS)` 原子 API。

### P2-2. Gateway CORS 生产环境配置过于宽松

- **文件**: `sca-skeleton-gateway-dev.yaml:88-93`
- **问题**: `allowed-origins: "*"` + `allowed-methods: "*"` + `allowed-headers: "*"`。
- **风险**: 中。任意站点可发起跨域请求（尽管 OAuth 流程以整页跳转为主，但 API 请求会被影响）。
- **建议**: 生产模式下（`application-prod.yaml`）限制为具体 SPA 域名，如 `https://admin.example.com, https://portal.example.com`。

### P2-3. 密码编码器包含 `{noop}` 明文选项

- **文件**: `AuthSecurityConfig.java:101-110`
- **问题**: `DelegatingPasswordEncoder` 中注册了 `{noop}` 编码器。若运维误在数据库存入 `{noop}plaintext`，密码将以明文校验。
- **风险**: 中。人为失误可导致所有密码明文暴露。
- **建议**: 仅保留 `{bcrypt}` 编码器；如确需开发便利，通过 `@Profile("dev")` 条件注入。

### P2-4. 授权码 TTL 未显式配置

- **文件**: `RegisteredClientInitializer.java:89-94`
- **问题**: `TokenSettings` 配置了 `accessTokenTtl` 和 `refreshTokenTtl`，但**未配置 `authorizationCodeTimeToLive`**，使用 SAS 默认值 5 分钟。5 分钟对于生产环境可能偏长。
- **风险**: 中。授权码窗口期长 → CSRF/Code Interception 攻击面增大。
- **建议**: 显式设置 `authorizationCodeTimeToLive(Duration.ofSeconds(60))`，缩短至 60 秒。

### P2-5. `SecurityConfig` 中 JWT 资源服务器配置未用于 Auth 服务

- **文件**: `AuthorizationServerConfig.java:102`
- **问题**: Auth 服务自身配置了 `.oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))`，但 Auth 服务的 `sca-skeleton-auth-dev.yaml` 已设置 `sca.security.resource-server.enabled: false`，且 Auth 服务并不依赖 `starter-security` 的 `RedisOpaqueTokenIntrospector`。JWT resource server 在此处仅用于 OIDC 端点验证 id_token，但未显式配置鉴权规则。
- **风险**: 低。JWT 配置存在但实际不被 auth service 的 endpoint 使用。
- **建议**: 移除或明确注释此配置目的，避免后续维护者困惑。

### P2-6. `LoginChannelContext` 默认回退 `ADMIN` 的隐式安全假设

- **文件**: `LoginChannelContext.java:32-35`
- **问题**: 当 ThreadLocal 未设置时（如 `LoginChannelFilter` 被绕过），默认回退 `ADMIN`。这可能导致非预期场景下（如直接调用 `RoutingUserDetailsService`）加载后台用户数据。
- **风险**: 中低。目前 `RoutingUserDetailsService` 仅在表单登录中被调用，而表单登录必经 `LoginChannelFilter`，实际风险可控。
- **建议**: 在 `RoutingUserDetailsService` 中增加 null 检查，明确失败回退策略。

---

## P3 — 建议优化

### P3-1. `CachingRegisteredClientRepository` 未清理过期 client_id 索引

- **文件**: `CachingRegisteredClientRepository.java:94-110`
- **描述**: `findByClientId()` 中 client_id 索引命中但 ID 缓存过期时，回源 JDBC 后重新缓存，但**未删除过期的 client_id 索引**。下次查询仍先查到该过期索引再 miss ID 缓存。
- **建议**: 当 `findById(id)` 返回 null 时，主动删除该 client_id 索引。

### P3-2. `SecurityUtils` 同时兼容 JWT 与 Opaque Token 增加维护复杂度

- **文件**: `SecurityUtils.java:44-55`
- **描述**: `getTokenAttributes()` 同时处理 `BearerTokenAuthentication` 和 `JwtAuthenticationToken`。当前架构仅使用不透明令牌，JWT 分支不会被执行。
- **建议**: 移除 JWT 分支，降低认知负载；未来需 JWT 时再恢复。

### P3-3. Session Cookie 域名/路径未显式配置

- **文件**: `SessionConfig.java` 不存在?
- **描述**: 未见 `server.servlet.session.cookie.domain` 或 `cookie.path` 配置。跨网关/服务的 Session Cookie 路径可能因默认值（当前请求路径）导致不一致。
- **建议**: 显式配置 `server.servlet.session.cookie.path=/`，生产环境配置 `cookie.domain=.example.com`（如适用）。

### P3-4. `accountNonLocked` 与 `enabled` 状态判断语义重叠

- **文件**: `ScaUserDetailsService.java:111-117`
- **描述**: `frozen` 状态同时使 `enabled=false` 和 `accountNonLocked=false`，但 Spring Security 对 `disabled` 的反馈信息是"账户被禁用"而非"被冻结"，语义不清。
- **建议**: 统一状态机：`frozen` → `accountNonLocked=true` + `enabled=false`；`locked` → `accountNonLocked=false` + `enabled=true`。

### P3-5. 全局异常信息泄漏风险

- **文件**: `RedisOpaqueTokenIntrospector.java:55-57`
- **描述**: `BadOpaqueTokenException("Invalid access token")` 的消息会返回给客户端，暴露令牌校验机制。
- **建议**: 统一为 `"Invalid token"` 或 `"401"`，避免透露内部细节。

---

## 合规性清单自查

| 要求 | 状态 | 备注 |
|------|------|------|
| PKCE S256 required | ✅ | `requireProofKey=true` |
| 无 client_secret | ✅ | `ClientAuthenticationMethod.NONE` |
| Refresh Token Rotation | ✅ | `reuseRefreshTokens=false` |
| 不透明令牌 | ✅ | `OAuth2TokenFormat.REFERENCE` |
| 授权码一次性使用 | ✅ | SAS 默认行为 |
| CSRF 防护（登录页） | ✅ | Thymeleaf `_csrf` |
| State 参数验证 | ✅ | 前端 state + server session |
| 令牌过期清理 | ✅ | Redis TTL |
| 速率限制（Token） | ⚠️ | 限流但仅 Gateway 层 |
| 速率限制（Authorize） | ❌ | **缺失** |
| 验证码 | ❌ | **模块未集成** |
| DPoP / mTLS | ❌ | **未实现** |
| 令牌吊销 | ❌ | **端点未放行网关** |
| 密码强度策略 | ⚠️ | 仅 bcrypt，无密码历史检查 |
| 账号锁定自动解锁 | ✅ | `LoginAttemptService` |
| 审计日志 | ⚠️ | 事件监听存在，但未持久化到 `sys_login_log` |

---

## 总结

- **P0**: 1 项 — Token 吊销端点未放行网关。
- **P1**: 3 项 — Captcha 未集成、DPoP 缺失、Authorize 端点无限流。
- **P2**: 6 项 — 含 Redis TTL 原子性、CORS 过松、密码编码器、授权码 TTL 等。
- **P3**: 5 项 — 优化建议。

建议优先处理 P0 和 P1 后再上线生产环境。
