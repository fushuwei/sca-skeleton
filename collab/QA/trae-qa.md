# TRAE 评审报告：授权码+PKCE → 密码模式改造

> **评审对象**：GLM 完成的认证授权模式改造（commit `5dc0d434`，85 个文件，+2344 / -3534 行）
>
> **评审日期**：2026-07-26
>
> **评审人**：TRAE (GLM-5.2)
>
> **结论**：改造架构设计优于 pig 项目，代码规范性良好，但存在 **3 个阻断性缺陷（CRITICAL）** 导致 portal 登录完全不可用、登录日志与账号锁定功能静默失效。**必须在合并前修复。**

---

## 目录

1. [评审方法论](#1-评审方法论)
2. [问题分级总览](#2-问题分级总览)
3. [阻断性问题（CRITICAL）](#3-阻断性问题critical)
4. [重要问题（MAJOR）](#4-重要问题major)
5. [一般问题（MINOR）](#5-一般问题minor)
6. [设计亮点](#6-设计亮点)
7. [与已有 QA 报告的交叉验证](#7-与已有-qa-报告的交叉验证)
8. [评审结论](#8-评审结论)

---

## 1. 评审方法论

本次评审基于对 commit `5dc0d434` 全部 85 个变更文件的逐行阅读，重点验证以下维度：

- **认证流程正确性**：从 SPA 发送请求 → Gateway 路由 → CaptchaVerificationFilter → SAS 客户端认证 → 密码模式 Converter → 密码模式 Provider → AuthenticationManager → RoutingUserDetailsService → 令牌生成的完整链路
- **Spring Security 事件机制**：验证 `ProviderManager` 的事件发布是否正常驱动 `LoginLogPublisher` 和 `LoginAttemptEventListener`
- **已有功能不受影响**：验证登录日志、账号锁定、租户管理、用户管理等既有功能是否正常
- **前后端契约一致性**：验证前端发送的请求参数与后端过滤器的读取方式是否匹配

评审过程中阅读的关键文件包括但不限于：
- 后端：`OAuth2ResourceOwnerBaseAuthenticationProvider.java`、`OAuth2ResourceOwnerPasswordAuthenticationProvider.java`、`CaptchaVerificationFilter.java`、`AuthorizationServerConfig.java`、`LoginLogPublisher.java`、`LoginAttemptEventListener.java`、`LoginAttemptService.java`、`RoutingUserDetailsService.java`、`OAuth2RegisteredClientInitializer.java`
- 前端：`password-grant.ts`、`axios-oauth.ts`、`LoginView.vue`（admin + portal）、`stores/auth.ts`（admin + portal）、`router/guards.ts`
- 配置/SQL：`sca-skeleton-auth-dev.yaml`、`sca-skeleton-gateway-dev.yaml`、`sca_platform.sql`

---

## 2. 问题分级总览

| 编号 | 严重程度 | 类别 | 问题 | 已有报告是否发现 |
|------|---------|------|------|----------------|
| C-1 | 🔴 CRITICAL | 功能阻断 | `resolveLoginChannel` 无法从 Basic auth 头获取 client_id，portal 登录完全不可用 | ❌ 均未发现 |
| C-2 | 🔴 CRITICAL | 安全绕过 | `CaptchaVerificationFilter` 无法识别 portal 客户端，验证码校验被静默跳过 | ❌ 均未发现 |
| C-3 | 🔴 CRITICAL | 功能失效 | `ProviderManager` 使用 `NullEventPublisher`，登录日志和账号锁定功能静默失效 | ❌ 均未发现 |
| M-1 | 🟡 MAJOR | 安全 | `client_secret` 暴露在前端 bundle 中 | ✅ Claude 发现 |
| M-2 | 🟡 MAJOR | 残留 | Thymeleaf 依赖未移除 | ✅ Claude 发现 |
| M-3 | 🟡 MAJOR | 逻辑 | "记住我"复选框无实际功能 | ✅ Claude 发现 |
| M-4 | 🟡 MAJOR | 残留 | `AuthLoginProperties` 成为死代码 | ❌ 均未发现 |
| M-5 | 🟡 MAJOR | 一致性 | SQL `token_settings` JSON 不完整，依赖 Initializer 自愈 | ✅ Claude 发现 |
| M-6 | 🟡 MAJOR | 安全 | Token 吊销为 fire-and-forget | ✅ Claude 发现 |
| m-1 | 🟢 MINOR | 文档 | 3 处 Javadoc 引用已删除的 `LoginChannelFilter` | ✅ Claude 发现 |
| m-2 | 🟢 MINOR | 代码 | `resolveLoginChannel` 注释矛盾 + 死代码 `AuthenticationProvider self = this;` | ✅ Claude 发现 |
| m-3 | 🟢 MINOR | 代码 | `@SuppressWarnings("deprecation")` 缺少说明 | ✅ Claude 发现 |
| m-4 | 🟢 MINOR | 代码 | `authorization_code` 转换器冗余注册 | ✅ Claude 发现 |
| m-5 | 🟢 MINOR | 一致性 | `writeCaptchaError` 未做 JSON 转义（与 EntryPoint 不一致） | ❌ 均未发现 |
| m-6 | 🟢 MINOR | 残留 | `spring-boot-starter-session-data-redis` 依赖在无状态模式下可能冗余 | ❌ 均未发现 |

---

## 3. 阻断性问题（CRITICAL）

### C-1：`resolveLoginChannel` 无法获取 client_id，portal 登录完全不可用

**严重程度**：🔴 CRITICAL（功能阻断）

**影响**：所有 portal 用户（`realm=portal`）无法登录

**文件**：
- [OAuth2ResourceOwnerPasswordAuthenticationProvider.java](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/grant/password/OAuth2ResourceOwnerPasswordAuthenticationProvider.java#L85-L97)
- [OAuth2ResourceOwnerBaseAuthenticationConverter.java](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/grant/base/OAuth2ResourceOwnerBaseAuthenticationConverter.java#L106-L110)
- [password-grant.ts](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-frontend/packages/shared/src/oauth/password-grant.ts#L85-L96)

**根因分析**：

机密客户端使用 `client_secret_basic` 认证方式时，`client_id` 和 `client_secret` 通过 `Authorization: Basic base64(client_id:client_secret)` 请求头发送，**不在请求体中**。前端 `loginWithPassword()` 函数的请求体仅包含 `grant_type`、`username`、`password`、`scope`（及可选的验证码参数），不包含 `client_id`：

```typescript
// password-grant.ts:85-96
const body = new URLSearchParams({
    grant_type: "password",
    username,
    password,
    scope: config.scope
});
// client_id 仅在 Authorization 头中，不在 body 中
```

后端 `OAuth2ResourceOwnerBaseAuthenticationConverter` 构建 `additionalParameters` 时，从 `request.getParameterMap()` 提取所有参数（排除 `grant_type` 和 `scope`）。由于 `client_id` 不在请求体中，`additionalParameters` 中**不包含 `client_id`**。

`OAuth2ResourceOwnerPasswordAuthenticationProvider.resolveLoginChannel()` 尝试从 `reqParameters.get("client_id")` 获取客户端 ID：

```java
// OAuth2ResourceOwnerPasswordAuthenticationProvider.java:92-96
Object clientId = reqParameters.get("client_id");  // 永远返回 null
if (clientId != null && oauth2ClientProperties.getPortal().getClientId().equals(clientId)) {
    return LoginChannel.PORTAL;  // 永远不会执行
}
return LoginChannel.ADMIN;  // 永远返回 ADMIN
```

**故障链路**：

```
Portal SPA 发送登录请求
  └─ Authorization: Basic base64(sca-portal-client:portal-secret)  ← client_id 在头中
  └─ Body: grant_type=password&username=...&password=...&scope=...

密码模式 Provider.resolveLoginChannel(reqParameters)
  └─ reqParameters.get("client_id") → null  ← 请求体中没有 client_id
  └─ 返回 LoginChannel.ADMIN  ← 错误！应为 PORTAL

LoginChannelContext.set(ADMIN)

RoutingUserDetailsService.loadUserByUsername(username)
  └─ LoginChannelContext.get() → ADMIN
  └─ 调用 scaUserDetailsService.loadUserByUsername(username)
  └─ 查询 realm=admin 的用户  ← portal 用户在 realm=portal，查不到

结果：UsernameNotFoundException → portal 用户无法登录
```

**修复方向**：

`resolveLoginChannel` 不应从 `additionalParameters` 获取 `client_id`，而应使用已认证客户端的信息。基础 Provider 的 `authenticate()` 方法中已经获取了 `registeredClient`，应将其传递给 `resolveLoginChannel`：

```java
// 基础 Provider 修改建议
LoginChannel channel = resolveLoginChannel(reqParameters, registeredClient);

// 密码模式 Provider 修改建议
@Override
public LoginChannel resolveLoginChannel(Map<String, Object> reqParameters,
                                        RegisteredClient registeredClient) {
    if (oauth2ClientProperties.getPortal().getClientId()
            .equals(registeredClient.getClientId())) {
        return LoginChannel.PORTAL;
    }
    return LoginChannel.ADMIN;
}
```

---

### C-2：`CaptchaVerificationFilter` 无法识别 portal 客户端，验证码校验被静默跳过

**严重程度**：🔴 CRITICAL（安全绕过）

**影响**：portal 渠道的图形验证码保护完全失效，攻击者可无限制暴力尝试密码

**文件**：[CaptchaVerificationFilter.java](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/security/filter/CaptchaVerificationFilter.java#L75-L80)

**根因分析**：

与 C-1 同源。`CaptchaVerificationFilter` 通过 `request.getParameter("client_id")` 获取客户端 ID 来判断是否为 portal 渠道：

```java
// CaptchaVerificationFilter.java:76-80
String clientId = request.getParameter(PARAM_CLIENT_ID);  // Basic auth 模式下返回 null
if (!isPortalClient(clientId)) {
    filterChain.doFilter(request, response);  // 直接放行，跳过验证码校验
    return;
}
```

Servlet API 的 `getParameter()` 仅从 URL 查询字符串和表单编码的请求体中读取参数，**不从 HTTP 头中读取**。当使用 `client_secret_basic` 认证方式时，`client_id` 在 `Authorization` 头中，`getParameter("client_id")` 返回 `null`。

`isPortalClient(null)` 返回 `false`（因为配置中的 portal clientId 是 `"sca-portal-client"`，不等于 `null`），过滤器直接放行，**跳过验证码校验**。

**安全影响**：

验证码是 portal 渠道防止暴力破解的关键控制。此缺陷使得攻击者可以：
1. 直接调用 `/auth/oauth2/token` 端点，无需验证码即可暴力尝试用户名/密码组合
2. 配合 C-1 的修复（如果仅修复 C-1 而不修复 C-2），验证码仍会被跳过

**修复方向**：

方案一（推荐）：从 `Authorization` 头解析 Basic 认证获取 `client_id`
方案二：将过滤器移至 SAS 客户端认证之后，从 `SecurityContext` 中获取已认证的 `OAuth2ClientAuthenticationToken`
方案三：在请求体中额外携带 `login_channel` 参数（不依赖 `client_id`）

---

### C-3：`ProviderManager` 使用 `NullEventPublisher`，登录日志和账号锁定功能静默失效

**严重程度**：🔴 CRITICAL（功能失效）

**影响**：
1. **登录日志完全不记录** — `LoginLogPublisher` 的 `@EventListener` 方法永远不会被触发
2. **账号锁定功能失效** — `LoginAttemptEventListener` 的 `@EventListener` 方法永远不会被触发，连续登录失败不会锁定账号
3. 两个功能都是**静默失效**（不报错、不抛异常），难以在测试中发现

**文件**：
- [AuthorizationServerConfig.java](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/config/AuthorizationServerConfig.java#L164-L168) — `authenticationManager()` 方法
- [LoginLogPublisher.java](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/security/LoginLogPublisher.java#L70-L91) — 依赖 `AuthenticationSuccessEvent` / `AbstractAuthenticationFailureEvent`
- [LoginAttemptEventListener.java](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/security/LoginAttemptEventListener.java#L27-L54) — 依赖 `AuthenticationSuccessEvent` / `AbstractAuthenticationFailureEvent`
- [AuthorizationServerConfig.java](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/config/AuthorizationServerConfig.java#L164-L168) — `new ProviderManager(provider)` 未注入事件发布器

**根因分析**：

`AuthorizationServerConfig.authenticationManager()` 手动构造 `ProviderManager`：

```java
// AuthorizationServerConfig.java:164-168
private AuthenticationManager authenticationManager() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(routingUserDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return new ProviderManager(provider);  // ← 未设置 AuthenticationEventPublisher
}
```

`ProviderManager` 默认使用 `NullEventPublisher`（内部空实现），认证成功/失败时不发布任何事件。`LoginLogPublisher` 和 `LoginAttemptEventListener` 的 `@EventListener` 方法永远不会被触发。

**关键细节**：`authenticationManager()` 是 `private` 方法，返回的 `ProviderManager` 不是 Spring 管理的 Bean，因此 Spring Boot 的自动装配（`AuthenticationConfiguration`）不会为其注入 `ApplicationEventPublisher`。

**影响链路**：

```
ProviderManager.authenticate()
  ├─ 认证成功 → eventPublisher.publishAuthenticationSuccess()  → NullEventPublisher（空操作）
  └─ 认证失败 → eventPublisher.publishAuthenticationFailure()  → NullEventPublisher（空操作）

LoginLogPublisher.onAuthenticationSuccess()    ← 永远不触发
LoginLogPublisher.onAuthenticationFailure()    ← 永远不触发
LoginAttemptEventListener.onAuthenticationSuccess() ← 永远不触发
LoginAttemptEventListener.onAuthenticationFailure() ← 永远不触发

结果：
  1. sys_login_log 表永远没有新记录
  2. 连续登录失败不会触发 LoginAttemptService.onLoginFailure()，账号永不锁定
  3. 两项功能均静默失效，不报错、不抛异常，测试中难以发现
```

**修复方向**：

注入 `ApplicationEventPublisher`，为 `ProviderManager` 设置 `DefaultAuthenticationEventPublisher`：

```java
// AuthorizationServerConfig 需注入 ApplicationEventPublisher
private final ApplicationEventPublisher applicationEventPublisher;

private AuthenticationManager authenticationManager() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(routingUserDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    ProviderManager providerManager = new ProviderManager(provider);
    providerManager.setAuthenticationEventPublisher(
        new DefaultAuthenticationEventPublisher(applicationEventPublisher));
    return providerManager;
}
```

**验证方法**：修复后登录一次（成功 + 失败各一次），检查 `sys_login_log` 表是否有新记录、连续失败 5 次后账号是否被锁定。

---

## 4. 重要问题（MAJOR）

### M-1：`client_secret` 暴露在前端 bundle 中

**严重程度**：🟡 MAJOR（安全）

**文件**：[password-grant.ts](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-frontend/packages/shared/src/oauth/password-grant.ts#L46-L49)

**问题**：`client_secret` 通过 `VITE_OAUTH_CLIENT_SECRET` 环境变量注入前端，经 Vite 构建后明文嵌入 JS bundle。任何用户都可通过浏览器开发者工具查看 `Authorization: Basic base64(client_id:client_secret)` 头中的密钥。

**与 pig 项目的对比**：pig 项目同样存在此问题（前端硬编码 client_secret），本次改造未在此点上超越 pig。

**风险**：攻击者获取 `client_secret` 后可绕过 SPA 直接调用 `/oauth2/token` 端点。但由于密码模式仍需用户名+密码，且 portal 渠道有验证码保护（C-2 修复后），实际风险可控。此问题是密码模式 + SPA 架构的固有妥协（OAuth2 密码模式规范上要求机密客户端，但 SPA 天然无法保密）。

**缓解方向**（可选，不阻断合并）：
1. 为 portal 客户端配置独立的低权限 `client_secret`，与 admin 客户端隔离
2. 在 Gateway 层对 `/oauth2/token` 端点增加 IP 限流
3. 长期方案：迁移到 BFF（Backend for Frontend）模式，由 BFF 持有 `client_secret`

---

### M-2：Thymeleaf 依赖未移除

**严重程度**：🟡 MAJOR（残留）

**文件**：[pom.xml](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/pom.xml#L69-L73)

**问题**：改造已删除所有 Thymeleaf 模板和静态资源，但 `spring-boot-starter-thymeleaf` 依赖仍保留在 `pom.xml` 中（第 69-73 行）。Spring Boot 自动装配仍会初始化 Thymeleaf 视图解析器等组件，造成不必要的资源开销和启动时间增加。

**修复方向**：删除 `pom.xml` 中的 `spring-boot-starter-thymeleaf` 依赖。

---

### M-3："记住我"复选框无实际功能

**严重程度**：🟡 MAJOR（逻辑）

**问题**：登录页保留了"记住我"复选框 UI，但密码模式下无对应后端逻辑。原授权码模式下可能通过 Spring Security 的 `RememberMeServices` 实现，改造后未迁移。

**影响**：用户勾选"记住我"后无任何效果（refresh_token 的 TTL 由 `OAuth2ClientProperties` 统一配置，不受此选项影响），构成误导性 UI。

**修复方向**：要么移除复选框，要么实现差异化的 refresh_token TTL（勾选时延长 TTL，不勾选时缩短或不下发 refresh_token）。

---

### M-4：`AuthLoginProperties` 成为死代码

**严重程度**：🟡 MAJOR（残留）

**文件**：
- [AuthLoginProperties.java](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/config/properties/AuthLoginProperties.java) — 整个类
- [AuthorizationServerConfig.java](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/config/AuthorizationServerConfig.java#L70) — `@EnableConfigurationProperties` 仍引用

**问题**：`AuthLoginProperties` 原为 Thymeleaf 登录页注入配置（系统名称、Logo、版权、轮播图、表单提交 URL）。改造删除了所有 Thymeleaf 模板后，此类不再被任何代码消费，但仍在 `AuthorizationServerConfig` 的 `@EnableConfigurationProperties` 中注册。

**修复方向**：删除 `AuthLoginProperties.java`，并从 `@EnableConfigurationProperties` 中移除引用。对应配置项 `sca.auth.login.*` 也应从 YAML 中清理。

---

### M-5：SQL `token_settings` JSON 不完整，依赖 Initializer 自愈

**严重程度**：🟡 MAJOR（一致性）

**文件**：[sca_platform.sql](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/deploy/sql/install/sca_platform.sql#L676-L677)

**问题**：SQL 中 `token_settings` 仅包含 `{"settings.token.reuse-refresh-tokens":false}`，缺失：
- `settings.token.access-token-format`（应为 `{"value":"reference"}` 以生成不透明令牌）
- `settings.token.access-token-time-to-live`
- `settings.token.refresh-token-time-to-live`

SAS 反序列化时缺失字段使用默认值（`access-token-format` 默认为 `SELF_CONTAINED` 即 JWT），与项目硬约束「使用不透明 access token」冲突。

**当前自愈机制**：`OAuth2RegisteredClientInitializer.migrateToOpaqueAccessTokenIfNeeded()` 检测到 token 格式非 REFERENCE 时会重建 `TokenSettings`。同时 SQL 中 `client_secret = NULL`，Initializer 的 `migratePublicClientIfNeeded` 检测到公共客户端后会整体重建，顺带修正 token_settings。

**风险**：若有人在 Initializer 未运行的环境（如单元测试、手动 SQL 初始化后跳过启动）中使用该客户端，会生成 JWT 而非不透明令牌，违反安全约束。

**修复方向**：在 SQL 中补全 `token_settings` JSON：
```json
{"settings.token.access-token-format":{"value":"reference"},
 "settings.token.access-token-time-to-live":3600.000000000,
 "settings.token.refresh-token-time-to-live":604800.000000000,
 "settings.token.reuse-refresh-tokens":false}
```

---

### M-6：Token 吊销为 fire-and-forget

**严重程度**：🟡 MAJOR（安全）

**文件**：[password-grant.ts](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-frontend/packages/shared/src/oauth/password-grant.ts#L157-L169)

**问题**：`revokeOAuthToken()` 使用 `try-catch` 吞掉所有异常，吊销失败时本地仍清理 token。若网络波动导致 revoke 请求未到达服务端，服务端的 access_token/refresh_token 仍然有效，用户实际未登出。

```typescript
try {
    await fetch(config.revokeUrl, { ... });
} catch {
    // logout 场景下 revoke 失败不阻断本地清理
}
```

**影响**：在安全敏感场景（如用户怀疑账号被盗主动登出），token 可能仍有效。

**修复方向**（可选）：对 revoke 失败进行日志记录或重试；或在服务端设置较短的 access_token TTL（如 30 分钟）作为兜底。

---

## 5. 一般问题（MINOR）

### m-1：3 处 Javadoc 引用已删除的 `LoginChannelFilter`

**文件**：
- [LoginLogPublisher.java#L169](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/security/LoginLogPublisher.java#L169) — "从请求属性取出 `LoginChannelFilter` 记录的开始时间"
- [RoutingUserDetailsService.java#L25](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/security/RoutingUserDetailsService.java#L25) — "Filter 已写入 ThreadLocal"
- [OAuth2ResourceOwnerBaseAuthenticationProvider.java#L60](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/grant/base/OAuth2ResourceOwnerBaseAuthenticationProvider.java#L60) — "移除对 `LoginChannelFilter` 的依赖"

**问题**：`LoginChannelFilter` 已在改造中删除，`LoginChannelContext` 现由 `OAuth2ResourceOwnerBaseAuthenticationProvider.authenticate()` 直接设置。但多处 Javadoc 仍引用 `LoginChannelFilter`，对新开发者产生误导。

**修复方向**：更新 Javadoc，将"LoginChannelFilter"改为"OAuth2ResourceOwnerBaseAuthenticationProvider"。

---

### m-2：`resolveLoginChannel` 注释矛盾 + 死代码

**文件**：[OAuth2ResourceOwnerPasswordAuthenticationProvider.java#L87-L97](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/grant/password/OAuth2ResourceOwnerPasswordAuthenticationProvider.java#L87-L97)

**问题**：
1. 注释自相矛盾：第 87 行说"附加参数中不包含 client_id"，第 90-91 行又说"实际上 client_id 会出现在 additionalParameters 中"
2. `AuthenticationProvider self = this;`（第 89 行）是死代码，从未使用

**修复方向**：删除矛盾注释和死代码。根因是 C-1（应改用 `registeredClient.getClientId()`），修复 C-1 后这些注释自然消失。

---

### m-3：`@SuppressWarnings("deprecation")` 缺少说明

**文件**：[AuthorizationServerConfig.java#L147](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/config/AuthorizationServerConfig.java#L147)

**问题**：`passwordAuthenticationProvider()` 方法标注了 `@SuppressWarnings("deprecation")` 但未说明抑制的具体是哪个废弃 API。`DaoAuthenticationProvider` 的 `DeprecationPostProcessor` 构造器在 Spring Security 6.2+ 中标记为 `@Deprecated`，但读者无法从注释得知。

**修复方向**：补充注释说明废弃来源，如 `// DaoAuthenticationProvider(userDetailsService) 构造器在 Spring Security 6.2+ @Deprecated`。

---

### m-4：`authorization_code` 转换器冗余注册

**文件**：[AuthorizationServerConfig.java#L123-L128](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/config/AuthorizationServerConfig.java#L123-L128)

**问题**：`accessTokenRequestConverter()` 注册了 `OAuth2AuthorizationCodeAuthenticationConverter`，但改造后客户端仅配置 `password` + `refresh_token` 授权类型，不再支持 `authorization_code`。此转换器永远不会匹配（请求中不会有 `grant_type=authorization_code`），属于冗余注册。

**修复方向**：可保留（作为向后兼容的基础设施），也可移除以减少不必要的转换器调用。建议保留但添加注释说明。

---

### m-5：`writeCaptchaError` 未做 JSON 转义（与 EntryPoint 不一致）

**文件**：[CaptchaVerificationFilter.java#L124-L131](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/security/filter/CaptchaVerificationFilter.java#L124-L131)

**问题**：`writeCaptchaError()` 直接将 `message` 拼入 JSON 字符串，未做转义。而 `AuthorizationServerConfig.oauth2TokenEndpointAuthenticationEntryPoint()` 对 `message` 做了 `\\`、`\"`、`\n`、`\r`、`\t` 转义。两处生成 OAuth2 JSON 错误响应的逻辑风格不一致。

当前 `writeCaptchaError` 的 `message` 均为硬编码中文字符串（"验证码不能为空"、"验证码错误，请重新输入"），不含特殊字符，无实际注入风险。但若后续扩展为动态消息，存在 JSON 注入隐患。

**修复方向**：统一两处 JSON 生成逻辑，提取为公共工具方法并统一转义。

---

### m-6：`spring-boot-starter-session-data-redis` 依赖在无状态模式下可能冗余

**文件**：[pom.xml#L63-L67](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/pom.xml#L63-L67)

**问题**：改造后认证为无状态模式（`SessionCreationPolicy.STATELESS`），不再依赖 HTTP Session。`spring-boot-starter-session-data-redis` 依赖可能冗余。

**需确认**：`RedisOAuth2AuthorizationService` 是否间接依赖 Spring Session 的 Redis 序列化机制。若 `RedisIndexedSessionRepository` 等 Session Bean 仍被其他组件引用，则不能直接移除。需运行时验证后决定。

**修复方向**：启动应用后检查是否存在 `SessionRepository` 相关 Bean；若无引用则移除依赖。

---

## 6. 设计亮点

本次改造在以下方面**优于 pig 项目**：

### 6.1 Provider 内部管理 LoginChannelContext，无需额外 Filter

pig 项目通过 `LoginChannelFilter` 在过滤器链中设置 ThreadLocal，存在 Filter 与 Provider 的时序耦合问题。本次改造将 `LoginChannelContext.set()` / `clear()` 收敛到 `OAuth2ResourceOwnerBaseAuthenticationProvider.authenticate()` 的 try-finally 中（[第 162 行 / 第 194 行](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/grant/base/OAuth2ResourceOwnerBaseAuthenticationProvider.java#L162)），职责更内聚，时序更确定。

### 6.2 异常映射使用标准 OAuth2 错误码

pig 项目在密码模式 Provider 中抛出自定义异常码。本次改造的 `mapToOAuth2AuthenticationException()` 将 Spring Security 认证异常映射为标准 OAuth2 错误码（`invalid_request`、`invalid_client`、`invalid_scope`、`server_error`），并附带 RFC 6749 错误 URI，符合 OAuth2 规范。

### 6.3 客户端迁移与自愈机制

`OAuth2RegisteredClientInitializer` 实现了完整的客户端生命周期管理：
- **损坏修复**：JSON 反序列化失败时删除重建
- **缺失初始化**：client_id 不存在时创建机密客户端
- **公共→机密迁移**：检测旧公共客户端并迁移为机密客户端
- **令牌格式迁移**：检测 JWT 格式并升级为不透明令牌（REFERENCE）

pig 项目通常只提供一次性初始化，缺乏迁移和自愈能力。

### 6.4 不透明令牌 + claims 自定义

`ScaOpaqueAccessTokenClaimsCustomizer` 为不透明令牌附加业务 claims（tenantId、userId、isSuperadmin 等），资源服务器通过 token introspection 获取。相比 pig 项目的 JWT 方案，避免了 token payload 中的敏感信息泄露，符合项目硬约束。

### 6.5 基础 Provider 抽象设计

`OAuth2ResourceOwnerBaseAuthenticationProvider<T>` + `OAuth2ResourceOwnerBaseAuthenticationConverter<T>` 的泛型抽象，使新增自定义授权模式（如 SMS 验证码登录）只需继承基类并实现 4 个抽象方法（`support`、`buildToken`、`supports`、`checkClient`），扩展性优于 pig 项目的单体 Provider 设计。

### 6.6 登录日志的失败路径用户反查

`LoginLogPublisher.onAuthenticationFailure()` 在 SecurityContext 未建立时，通过 username + realm 反查 `sys_user` 填充 tenantId/userId。用户不存在时反查返回 null，留空而非报错。此设计确保即使登录失败（用户名不存在、密码错误）也有审计记录，且 `username` 字段始终记录原始输入。pig 项目通常只在成功路径记录日志。

---

## 7. 与已有 QA 报告的交叉验证

本次评审与 `collab/QA/` 目录下已有报告进行交叉验证：

| 问题 | trae (本次) | claude | cline | opencode | qoder | windsurf |
|------|:---:|:---:|:---:|:---:|:---:|:---:|
| C-1 portal 登录不可用 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| C-2 验证码静默跳过 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| C-3 事件发布失效 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| M-1 client_secret 暴露 | ✅ | ✅ | — | — | — | — |
| M-2 Thymeleaf 残留 | ✅ | ✅ | — | — | — | — |
| M-3 "记住我"无效 | ✅ | ✅ | — | — | — | — |
| M-4 AuthLoginProperties 死代码 | ✅ | ❌ | — | — | — | — |
| M-5 SQL token_settings 不完整 | ✅ | ✅ | — | — | — | — |
| M-6 revoke fire-and-forget | ✅ | ✅ | — | — | — | — |
| m-1 Javadoc 引用已删除 Filter | ✅ | ✅ | — | — | — | — |
| m-2 注释矛盾 + 死代码 | ✅ | ✅ | — | — | — | — |
| m-3 @SuppressWarnings 无说明 | ✅ | ✅ | — | — | — | — |
| m-4 authorization_code 冗余 | ✅ | ✅ | — | — | — | — |
| m-5 JSON 转义不一致 | ✅ | ❌ | — | — | — | — |
| m-6 session-data-redis 冗余 | ✅ | ❌ | — | — | — | — |

**关键发现**：3 个 CRITICAL 问题（C-1、C-2、C-3）均为本次评审**首次发现**，已有报告均未识别。这三个问题中任何一个都会导致核心功能不可用，说明已有报告的评审深度不足或未实际运行验证。

C-1 和 C-2 的根因相同：**机密客户端使用 `client_secret_basic` 时，`client_id` 在 HTTP 头而非请求体中**，但后端代码尝试从请求参数（`getParameter` / `additionalParameters`）获取 `client_id`。这是对 OAuth2 机密客户端认证机制的误解，属于设计层面缺陷。

C-3 的根因是 **`ProviderManager` 的 `NullEventPublisher` 默认行为**，这是一个 Spring Security 的常见陷阱：手动 `new ProviderManager()` 不会自动注入 `ApplicationEventPublisher`，必须显式设置。

---

## 8. 评审结论

### 总体评价

本次改造的**架构设计和代码规范性优于 pig 项目**，体现在：
- Provider 内部管理 ThreadLocal（无 Filter 耦合）
- 标准 OAuth2 错误码映射
- 完善的客户端迁移与自愈机制
- 不透明令牌 + claims 自定义（安全合规）
- 泛型化的基础 Provider 抽象（扩展性强）
- 登录日志失败路径反查（审计完整）

但存在 **3 个阻断性缺陷**导致核心功能不可用，**必须在合并前修复**：

| 编号 | 问题 | 影响 | 修复难度 |
|------|------|------|---------|
| C-1 | `resolveLoginChannel` 无法获取 client_id | portal 登录完全不可用 | 低（改用 `registeredClient.getClientId()`） |
| C-2 | `CaptchaVerificationFilter` 无法识别 portal 客户端 | 验证码校验静默跳过 | 低（从 Authorization 头解析或移至客户端认证后） |
| C-3 | `ProviderManager` 未注入事件发布器 | 登录日志 + 账号锁定静默失效 | 低（注入 `DefaultAuthenticationEventPublisher`） |

### 修复优先级

1. **P0（阻断合并）**：C-1 → C-2 → C-3（三者关联，建议一并修复）
2. **P1（合并后尽快）**：M-2（Thymeleaf 残留）、M-4（死代码清理）、M-5（SQL 补全）
3. **P2（后续迭代）**：M-1（client_secret 架构性妥协）、M-3（记住我）、M-6（revoke 健壮性）
4. **P3（代码洁癖）**：m-1 ~ m-6（文档与代码一致性）

### 验证清单

修复 C-1/C-2/C-3 后，需执行以下验证：

- [ ] admin 渠道登录成功，`sys_login_log` 有成功记录
- [ ] admin 渠道登录失败（错误密码），`sys_login_log` 有失败记录，连续 5 次后账号锁定
- [ ] portal 渠道登录成功（含验证码），`sys_login_log` 有成功记录
- [ ] portal 渠道登录不填验证码 → 返回"验证码不能为空"
- [ ] portal 渠道登录填错验证码 → 返回"验证码错误，请重新输入"
- [ ] portal 渠道连续登录失败 5 次后账号锁定
- [ ] 令牌刷新（refresh_token）正常工作
- [ ] 退出登录（revoke）后 access_token 失效
- [ ] 现有功能回归：租户管理、用户管理、角色管理、菜单管理、操作日志正常

---

> **评审声明**：本报告基于对 commit `5dc0d434` 全部变更文件的逐行静态审查，未执行运行时验证。C-1/C-2/C-3 的根因分析基于 OAuth2 规范（RFC 6749）、Spring Security 6 源码和 SAS 实现机制的推理，建议修复后按上述验证清单进行实际运行验证。