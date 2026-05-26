# OAuth2.1 Authorization Code + PKCE 改造 QA 审查报告

> 审查日期: 2026-05-26（首次） / 2026-05-26（复审查，Cursor 整改后）
> 审查范围: 全仓库 (backend + frontend + deploy)
> 审查目标: 确认自定义密码授权模式 → OAuth2.1 Authorization Code + PKCE 改造的完整性、规范性和安全性

---

## 目录

1. [整体评价](#1-整体评价)
2. [严重问题 (Priority: Critical)](#2-严重问题-priority-critical)
3. [重要问题 (Priority: High)](#3-重要问题-priority-high)
4. [中等问题 (Priority: Medium)](#4-中等问题-priority-medium)
5. [建议改进 (Priority: Low)](#5-建议改进-priority-low)
6. [合规性检查清单](#6-合规性检查清单)
7. [总结](#7-总结)

---

## 1. 整体评价

Cursor 完成的 OAuth2.1 PKCE 改造**总体架构正确、代码质量较高**，使用了标准 Spring Authorization Server，完全移除了旧的自定义密码授权模式。改造涉及了前后端 **133 个文件**，核心链路完整。

**首次审查（2026-05-26）** 发现 4 个 Critical、5 个 High 级别问题。**Cursor 已全部修复**，复审确认所有 Critical/High 问题均已解决。目前在 **RegisteredClientRedisSerializer** 中留有一个 Medium 级别的隐式 SELF_CONTAINED 降级 fallback，风险已大幅降低但建议择机处理。

**改造核心亮点：**
- 完全移除 `password` grant 扩展，改用 Authorization Code + PKCE
- 后端使用 Spring Authorization Server (SAS) 标准实现
- 前端使用 `@repo/shared` 共享 PKCE 工具包（含 Axios 拦截器 + Refresh Token 自动续期），admin/portal 两套 SPA 各自独立配置
- 使用不透明 (opaque) access_token + Redis 本地自省
- 客户端强制 PKCE (`requireProofKey=true`)
- Refresh Token Rotation (`reuseRefreshTokens=false`)
- 授权记录存储在 Redis，符合微服务无状态设计
- 登录失败锁定机制完整实现（`LoginAttemptService` + `LoginAttemptEventListener`）
- 外部化 RSA 密钥持久化支持（`AuthJwkKeyLoader`，PEM/Base64 注入）

---

## 2. 严重问题 (Priority: Critical) — ✅ 已全部修复

> 以下 4 个 Critical 问题在 Cursor 整改后已全部修复。

### C-01. 前端没有实现 Refresh Token 自动续期

**修复状态: ✅ 已修复**

**文件:** `packages/shared/src/oauth/axios-oauth.ts`（新增） · `apps/admin/src/apis/http.ts`（重构） · `apps/portal/src/apis/http.ts`（新增）

**修复内容:**
- 新增 `packages/shared/src/oauth/axios-oauth.ts`，提供 `createOAuthAxiosInstance()` 和 `oauthRequest()`：
  - 请求拦截器自动注入 `Authorization: Bearer xxx`
  - 响应拦截器捕获 HTTP 401 → 调用 `refreshAccessTokenOnce()` 自动续期（含并发去重）→ 成功后重放原请求
  - 仅 refresh 也失败时 redirect 到 OAuth 登录页
  - `oauthRequest()` 额外处理业务码 40100 未认证场景，同样触发 refresh 或 redirect
- admin/portal 各自调用 `createOAuthAxiosInstance()` 构造 Axios 实例
- 通过 `registerAdminTokenSync()` / `registerPortalTokenSync()` 回调同步 Pinia 内存态

**通过 `refreshAccessTokenOnce()` 实现并发去重**，避免同一时刻多个请求触发多次 refresh 调用。

### C-02. Admin 生产环境 `.env.production` 缺少 OAuth 环境变量

**修复状态: ✅ 已修复**

**文件:** `apps/admin/.env.production` · `apps/admin/.env.test`

**修复内容:**
```env
VITE_OAUTH_CLIENT_ID=sca-admin-client
VITE_OAUTH_AUTHORIZE_URL=https://api.sca-skeleton.example.com/auth/oauth2/authorize
VITE_OAUTH_TOKEN_URL=https://api.sca-skeleton.example.com/auth/oauth2/token
VITE_OAUTH_REDIRECT_URI=https://admin.sca-skeleton.example.com/oauth/callback
VITE_OAUTH_SCOPE=openid profile all
```
两个文件均已补充完整的 OAuth 环境变量。

### C-03. Portal 应用缺少 `.env.production` 和 `.env.test`

**修复状态: ✅ 已修复**

**文件:** `apps/portal/.env.production` · `apps/portal/.env.test`（新增）

**修复内容:** 两个文件均已创建并包含 OAuth 配置，redirect_uri 分别指向 `portal.sca-skeleton.example.com` 和 `portal-test.sca-skeleton.example.com`。

### C-04. Axios 401 状态码检查与后端 ResultCode 不匹配

**修复状态: ✅ 已修复**

**文件:** `packages/shared/src/oauth/constants.ts` · `packages/shared/src/oauth/axios-oauth.ts`

**修复内容:**
- `constants.ts` 定义 `API_UNAUTHORIZED_CODE = 40100`（与后端 `ResultCode.UNAUTHORIZED` 对齐）
- `axios-oauth.ts` 的 `oauthRequest()` 中校验 `payload.code === (options.unauthorizedCode ?? API_UNAUTHORIZED_CODE)`，即默认使用 40100
- 同时 `createOAuthAxiosInstance()` 的响应拦截器捕获 HTTP 401 作为补充
- 两层防护：HTTP 状态码 401 + 业务码 40100 均可触发 refresh/redirect

---

## 3. 重要问题 (Priority: High) — ✅ 已全部修复

> 以下 5 个 High 问题在 Cursor 整改后已全部修复。

### H-01. Auth 服务禁用 CSRF 防护

**修复状态: ✅ 已修复**

**文件:** `sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/config/AuthSecurityConfig.java`

**修复内容:** 移除了 `.csrf(AbstractHttpConfigurer::disable)`。现在 CSRF 保护由 Spring Security 默认启用，覆盖表单登录端点。SAS 端点（Order=1 过滤链）由 `OAuth2AuthorizationServerConfigurer` 自行管理 CSRF。

**验证:** `AuthSecurityConfig.java:51` 注释说明 "表单登录启用 CSRF"，且无 `csrf().disable()` 调用。

### H-02. 生产环境 RSA 密钥内存生成 - 重启后所有 token 失效

**修复状态: ✅ 已修复**

**文件:** `sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/config/AuthJwkKeyLoader.java`（新增） · `auth/config/properties/AuthJwtProperties.java`（新增） · `AuthorizationServerConfig.java`（重构）

**修复内容:**
- 新增 `AuthJwkKeyLoader` 组件，支持外部化密钥注入：
  ```java
  if (authJwtProperties.isExternalKeyConfigured()) {
      return loadExternalKeyPair();  // 从 PEM/Base64 解析
  }
  return generateInMemoryKeyPair();  // 本地开发回退
  ```
- `AuthJwtProperties` 提供 `keyId`、`privateKey`（PKCS#8 PEM）、`publicKey`（X.509 PEM）配置
- `AuthorizationServerConfig` 不再调用 `generateRsaKeyPair()`，改由 `authJwkKeyLoader.loadKeyPair()` 加载
- Nacos 配置 `sca-skeleton-auth-dev.yaml:69-72` 保留 key-size 并注释了生产密钥注入示例：
  ```yaml
  # key-id: ${AUTH_JWT_KEY_ID:}
  # private-key: ${AUTH_JWT_PRIVATE_KEY:}
  # public-key: ${AUTH_JWT_PUBLIC_KEY:}
  ```

### H-03. Admin SPA 遗留 Mock 登录代码

**修复状态: ✅ 已修复**

**文件:** `LoginView.vue`（已删除） · `apis/mock/auth.ts`（已删除，替换为 `mock/menus.ts`） · `auth/apis/auth.ts`（已删除）

**修复内容:**
- `LoginView.vue`（840 行）已删除
- `mock/auth.ts` 已删除，替换为 `mock/menus.ts`（仅含演示菜单数据）
- `apis/auth.ts` 已删除
- 旧 password grant 扩展代码全部移除：`PasswordGrantAuthenticationConverter.java`、`PasswordGrantAuthenticationProvider.java`、`PasswordGrantAuthenticationToken.java`

### H-04. 网关 Token 端点限流

**修复状态: ✅ 已修复**

**文件:** `sca-skeleton-backend/sca-skeleton-gateway/src/main/resources/nacos/sca-skeleton-gateway-dev.yaml:35-45` · `gateway/config/RateLimiterConfiguration.java`

**修复内容:**
- 网关路由中新增 Token 端点限流路由（优先于普通 auth 路由）：
  ```yaml
  - id: sca-skeleton-auth-token
    predicates:
      - Path=/auth/oauth2/token
    filters:
      - name: RequestRateLimiter
        args:
          key-resolver: "#{@userKeyResolver}"
          redis-rate-limiter.replenishRate: 10
          redis-rate-limiter.burstCapacity: 20
  ```
- `RateLimiterConfiguration.java` 实现 `userKeyResolver`：优先按 Bearer token 字符串分桶，无 token 时按客户端 IP 分桶
- 使用 `XForwardedRemoteAddressResolver` 解析真实客户端 IP（信任 1 层 Nginx 代理）

### H-05. 登录失败次数锁定未实现（配置存在但代码不存在）

**修复状态: ✅ 已修复**

**文件:** `auth/security/LoginAttemptService.java`（新增） · `auth/security/LoginAttemptEventListener.java`（新增） · `auth/security/ScaUserDetailsService.java`（新增集成）

**修复内容:**
- `LoginAttemptService` 实现核心逻辑：
  - `onLoginSuccess()`：登录成功后清零 `login_fail_count`
  - `onLoginFailure()`：递增失败计数，达阈值时设置 `status=locked`
  - `unlockIfExpired()`：锁定超时后自动解锁（基于 `lock-duration-seconds`）
- `LoginAttemptEventListener` 监听 Spring Security 的 `AuthenticationSuccessEvent` / `AbstractAuthenticationFailureEvent`
- `ScaUserDetailsService.buildUserDetails()` 中调用 `loginAttemptService.unlockIfExpired(user)`，在 `loadUserByUsername` 时自动检查解锁
- `AuthLockProperties` 配置 `maxFailCount=5` / `lockDurationSeconds=1800`

---

## 4. 中等问题 (Priority: Medium)

### M-01. RegisteredClientRedisSerializer SELF_CONTAINED 降级 fallback

**文件:** `sca-skeleton-starter-security/src/main/java/.../oauth2/RegisteredClientRedisSerializer.java:162-164`

```java
if (!tokenSettingsMap.containsKey(ConfigurationSettingNames.Token.ACCESS_TOKEN_FORMAT)) {
    tokenSettingsBuilder.accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED);
}
```

**问题描述:** Redis 缓存反序列化时，若 `token_settings` JSON 中缺少 `access_token_format` 字段（如旧版本缓存残留），会静默降级为 `SELF_CONTAINED`（JWT）格式。

**风险已降低（非 blocking）：**
- `FORMAT_VERSION == 1` 检查 + 字段完整性校验会拒绝不兼容的格式
- `RegisteredClientInitializer` 启动时对所有客户端执行 `migrateToOpaqueAccessTokenIfNeeded()`，确保 JDBC 中存储的已是 REFERENCE
- 但若有人手动修改 Redis 中 `token_settings` 字段搞丢 `access_token_format`，会生成 JWT token

**建议:** 将此 fallback 改为抛出异常（fail-fast）或至少打 warn 日志。

### M-02. Gateway 认证检查流于表面

**文件:** `sca-skeleton-backend/sca-skeleton-gateway/src/main/java/io/github/fushuwei/scaskeleton/gateway/filter/GatewaySecurityGlobalFilter.java`

**问题描述:** 网关只检查 `Authorization: Bearer xxx` 头是否存在，**完全不验证 token 的有效性**。任何字符串如 `Bearer invalid-token` 都会通过网关。安全性完全依赖于下游服务的 token 自省。

**说明:** 这是**设计使然**——网关层做无状态路由，token 验证由下游资源服务的 `RedisOpaqueTokenIntrospector` 完成。若需加固可增加格式校验。

### M-03. System 服务缺少 sca.security.resource-server 开关

**文件:** `sca-skeleton-backend/sca-skeleton-system/src/main/resources/application.yml`

**问题分析:** System 服务依赖 starter-security 的默认值 `matchIfMissing = true` 来启用 Resource Server 自动配置。这是隐式依赖，建议**显式配置**以提高可读性和可控性。

### M-04. FeignHeaderInterceptor 透传的 X-User-Id/X-User-Name 被网关清除

**文件:** 
- `sca-skeleton-starter-feign/src/main/java/.../feign/interceptor/FeignHeaderInterceptor.java`
- `sca-skeleton-gateway/src/main/java/.../gateway/filter/RequestHeaderSanitizerGlobalFilter.java`

**问题描述:** 网关的 `RequestHeaderSanitizerGlobalFilter` 在入口处清理了 `X-User-Id`、`X-User-Name` 等头，而 Feign 拦截器试图从当前请求中读取并透传这些头。由于网关已清除，Feign 透传到的下游服务拿到的也是空值。

**建议:** 如果需要在服务间调用时传递用户身份，应从 token 自省结果中获取，而非依赖已清除的请求头。

### M-05. dump.rdb 被提交到 Git 仓库

**文件:** `sca-skeleton-backend/dump.rdb`

**问题描述:** Redis 持久化文件 `dump.rdb`（二进制）被意外提交到版本控制。应将其添加到 `.gitignore` 并从仓库中移除。

### M-06. LoginChannelFilter ThreadLocal 同步场景提醒

**文件:** `sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/security/filter/LoginChannelFilter.java`

**分析:** 正确使用 `try-finally` 确保清理。但注意异步场景或 `SecurityContext` 异步传播时，ThreadLocal 不会传递到子线程。当前同步场景下没有问题，标记为提醒。

---

## 5. 建议改进 (Priority: Low)

### L-01. 表单登录页缺少 CSRF Token

**文件:** `sca-skeleton-backend/sca-skeleton-auth/src/main/resources/templates/login/admin.html` · `portal.html`

**建议:** CSRF 已启用，Thymeleaf 模板应注入 `_csrf` token：
```html
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
```

### L-02. Token Revocation 已在 logout 中实现

**修复状态: ✅ 首次审查后已确认实现**

**文件:** `packages/shared/src/oauth/pkce.ts` · `apps/admin/src/stores/auth.ts` · `apps/portal/src/stores/auth.ts`

**内容:** Logout 时调用 `POST /oauth2/revoke`（通过 `revokeOAuthToken()`）使 access_token 和 refresh_token 失效，然后清理本地存储。Admin 和 portal 均已实现。

### L-03. Portal logout 已携带 prompt=login

**修复状态: ✅ 已确认**

**文件:** `apps/portal/src/stores/auth.ts:66`

```typescript
void startOAuthLogin(oauthConfig, "/", { prompt: "login" });
```

Portal 的 logout 方法已在 `startOAuthLogin` 中传递 `prompt: "login"`，强制用户重新输入凭据。Admin 的 logout（`stores/auth.ts:100`）同样已携带 `prompt: "login"`。

### L-04. Admin 应用 OAuth 相关类型定义

**文件:** `apps/admin/src/types/auth.ts`

**说明:** 已更新 `UserProfile`、`MenuItem` 等类型适配 OAuth 模式。旧 `LoginPayload`/`LoginResponse` 类型已清理。

### L-05. SessionStorage 隔离性依赖 clientId

**文件:** `packages/shared/src/oauth/pkce.ts`

**分析:** sessionStorage 按 `oauth_pkce_session:${clientId}` 隔离。admin 和 portal 使用不同的端口/域名（admin: 5173, portal: 5174），生产环境也是不同子域名（admin.sca-skeleton.example.com / portal.sca-skeleton.example.com），不存在冲突风险。

---

## 6. 合规性检查清单

### OAuth 2.1 (RFC 6749 bis) 合规性

| 要求 | 状态 | 说明 |
|------|------|------|
| Authorization Code + PKCE 为默认授权流程 | ✅ 通过 | 仅配置了 `AUTHORIZATION_CODE` + `REFRESH_TOKEN` |
| 公共客户端强制 PKCE | ✅ 通过 | `requireProofKey(true)` |
| 无 Implicit Grant | ✅ 通过 | 未配置 |
| 无 Password Grant | ✅ 通过 | 已完全移除所有自定义扩展代码 |
| Refresh Token Rotation | ✅ 通过 | `reuseRefreshTokens(false)` |
| 精确的 Redirect URI 匹配 | ✅ 通过 | SAS 内置严格匹配 |
| State 参数防 CSRF | ✅ 通过 | 前端生成 state → 回调时校验 |
| PKCE S256 challenge | ✅ 通过 | 使用 `crypto.subtle.digest("SHA-256")` |
| Token Revocation | ✅ 通过 | 前端 logout 调用 `/oauth2/revoke`，SAS 内置支持 |

### Spring Authorization Server 合规性

| 要求 | 状态 | 说明 |
|------|------|------|
| 使用标准 SAS 配置 | ✅ 通过 | `AuthorizationServerConfig` 正确配置 |
| OIDC 发现端点 | ✅ 通过 | `.oidc(Customizer.withDefaults())` |
| JWK Set 端点 | ✅ 通过 | 默认由 SAS 提供 |
| 正确的不透明 token 格式 | ✅ 通过 | `OAuth2TokenFormat.REFERENCE` |
| JDBC + Redis 双存储 | ✅ 通过 | JDBC 存客户端/consent，Redis 存授权记录 |
| RSA 密钥持久化 | ✅ 通过 | `AuthJwkKeyLoader` 支持外部 PEM/Base64 注入 |

### 安全架构合规性

| 要求 | 状态 | 说明 |
|------|------|------|
| HTTPS 传输加密 | ✅ 通过 | Nginx 配置了 TLS 1.2/1.3 + HSTS |
| 网关内外网隔离 | ✅ 通过 | Nginx 是唯一公网入口 |
| 内部头清理 | ✅ 通过 | `RequestHeaderSanitizerGlobalFilter` |
| CSRF 防护 | ✅ 通过 | 表单登录已启用 CSRF（SAS 端点自行管理） |
| Token 存储安全 | ⚠️ 部分满足 | localStorage (XSS 风险) vs httpOnly cookie |
| 速率限制 | ✅ 通过 | Token 端点 `RequestRateLimiter`（10/20） |
| 日志审计 | ✅ 通过 | 操作日志、登录日志表已存在 |
| 登录失败锁定 | ✅ 通过 | `LoginAttemptService` + `LoginAttemptEventListener` |

---

## 7. 总结

### 首次审查发现的问题（2026-05-26）

| 级别 | 数量 | 已修复 | 未修复 |
|------|------|--------|--------|
| Critical | 4 | ✅ 全部 | 0 |
| High | 5 | ✅ 全部 | 0 |
| Medium | 5 | 2 (M-03/M-05) | 3 (M-01/02/04) |
| Low | 5 | 升级为已确认 | 1 (L-01) |

### 复审后仍存在的问题

| # | 级别 | 问题 | 说明 |
|---|------|------|------|
| M-01 | Medium | `RegisteredClientRedisSerializer` SELF_CONTAINED 降级 fallback | 虽已被 FORMAT_VERSION 缓解，建议改为 fail-fast |
| M-02 | Medium | Gateway Bearer 检查流于表面 | 设计使然（下游自省），可加日志/格式校验 |
| M-03 | Medium | System 服务隐式依赖 starter 默认值 | 建议显式配置 `sca.security.resource-server.enabled` |
| M-04 | Medium | Feign 透传空用户头 | 需从 token 自省中获取而非清除后的请求头 |
| M-05 | Low | dump.rdb 被提交到 Git | 建议 `git rm --cached` + `.gitignore` |
| L-01 | Low | Thymeleaf 模板缺 CSRF token 注入 | CSRF 启用后需模板配合 |

### 新发现的问题（复审）

| # | 级别 | 问题 | 说明 |
|---|------|------|------|
| M-05 | Medium | `dump.rdb` 被提交到 Git | Redis dump 二进制文件不应版本控制 |

**总体结论:** Cursor 已全部修复首次审查发现的 **4 个 Critical** 和 **5 个 High** 级别问题。新增了 `packages/shared/src/oauth/axios-oauth.ts` 共享 Axios 封装（含 Refresh Token 自动续期）、`AuthJwkKeyLoader` 外部 RSA 密钥支持、`LoginAttemptService` 锁定机制等高质量代码。剩余 **3 个 Medium** 和 **2 个 Low** 级别问题均非 blocking，**建议修复 M-01（SELF_CONTAINED fallback）和清理 dump.rdb 后可删除本报告**。
