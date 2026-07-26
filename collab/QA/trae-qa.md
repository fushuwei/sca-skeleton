# Trae 评审报告：授权码+PKCE → 密码模式改造

> **评审对象**：GLM 完成的认证授权模式改造（commit `5dc0d434` ~ `7108e7f2`，91 个文件，+5206 / -3534 行）
>
> **评审基线**：`origin/1.0.0` vs `main` 分支
>
> **评审日期**：2026-07-26
>
> **评审人**：Trae（GLM-5.2）
>
> **结论**：改造架构设计与代码规范性优于 pig 项目，但存在 **3 个 CRITICAL 阻断性缺陷**（portal 登录不可用、验证码校验失效、登录日志/账号锁定静默失效）和 **1 个 CRITICAL 设计缺陷**（事件监听器类型检查不匹配），**必须在合并前修复**。

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

本次评审基于以下维度：

- **代码逐行审查**：对 commit `5dc0d434` ~ `7108e7f2` 全部 91 个变更文件进行逐行阅读
- **运行时链路推演**：从 SPA 发送请求 → Gateway 路由 → CaptchaVerificationFilter → SAS 客户端认证 → 密码模式 Converter → 密码模式 Provider → AuthenticationManager → RoutingUserDetailsService → 令牌生成 → 事件发布 → LoginLogPublisher/LoginAttemptEventListener 的完整链路推演
- **Spring Security 6 / SAS 源码机制**：验证 `ProviderManager` 的事件发布机制、`OAuth2ClientAuthenticationFilter` 的客户端认证时序、`DaoAuthenticationProvider` 的异常处理行为
- **前后端契约一致性**：验证前端 `password-grant.ts` 发送的请求参数与后端 `CaptchaVerificationFilter` / `OAuth2ResourceOwnerBaseAuthenticationConverter` 的读取方式是否匹配
- **与 6 份已有 QA 报告交叉验证**：对 catpawai/claude/cline/opencode/qoder/windsurf 的发现进行代码级验证，确认是否成立

---

## 2. 问题分级总览

| 编号  | 严重程度     | 类别     | 问题                                                            | 首次发现者       |
|-------|------------|----------|-----------------------------------------------------------------|------------------|
| T-C1  | 🔴 CRITICAL | 功能阻断 | `resolveLoginChannel` 无法获取 client_id，portal 登录完全不可用 | CatPawAI/Cline/Qoder |
| T-C2  | 🔴 CRITICAL | 安全绕过 | `CaptchaVerificationFilter` 无法识别 portal 客户端，验证码校验被静默跳过 | CatPawAI/Cline/Qoder |
| T-C3  | 🔴 CRITICAL | 功能失效 | `ProviderManager` 使用 `NullEventPublisher`，登录日志/账号锁定静默失效 | CatPawAI          |
| T-C4  | 🔴 CRITICAL | 功能失效 | 事件监听器类型检查不匹配，即使修复 T-C3 也无法触发                            | **Trae 独有**     |
| T-M1  | 🟡 MAJOR    | 安全     | `client_secret` 暴露在前端 bundle 中                              | 6 份报告均发现    |
| T-M2  | 🟡 MAJOR    | 残留     | Thymeleaf 依赖未移除                                             | Claude/CatPawAI  |
| T-M3  | 🟡 MAJOR    | 逻辑     | "记住我"复选框无实际功能                                           | 5 份报告均发现    |
| T-M4  | 🟡 MAJOR    | 残留     | `AuthLoginProperties` 成为死代码                                  | CatPawAI         |
| T-M5  | 🟡 MAJOR    | 一致性   | SQL `token_settings` JSON 不完整，依赖 Initializer 自愈           | Claude/CatPawAI/Qoder |
| T-M6  | 🟡 MAJOR    | 安全     | Token 吊销为 fire-and-forget                                     | Claude/CatPawAI  |
| T-M7  | 🟡 MAJOR    | 逻辑     | `LoginLogPublisher` 失败路径 `LoginChannelContext` 已被清理       | Cline            |
| T-M8  | 🟡 MAJOR    | 代码     | `onFirstKeyFocus` 绕过 Vue 响应式直接操作 DOM                     | Cline            |
| T-M9  | 🟡 MAJOR    | 残留     | `ScaRefreshTokenGenerator` 死代码                                | Qoder            |
| T-M10 | 🟡 MAJOR    | 安全     | 用户名枚举漏洞（代码层面）                                         | Qoder            |
| T-M11 | 🟡 MAJOR    | 安全     | Token 存储在 localStorage，XSS 可窃取                              | Qoder            |
| T-m1  | 🟢 MINOR    | 文档     | 多处 Javadoc 引用已删除的 `LoginChannelFilter`                    | 5 份报告均发现    |
| T-m2  | 🟢 MINOR    | 代码     | `resolveLoginChannel` 注释矛盾 + 死代码 `AuthenticationProvider self = this;` | Claude/CatPawAI/Cline |
| T-m3  | 🟢 MINOR    | 代码     | `@SuppressWarnings("deprecation")` 缺少说明                      | Claude/CatPawAI/Cline/OpenCode/Qoder |
| T-m4  | 🟢 MINOR    | 代码     | `authorization_code` 转换器冗余注册                               | Claude/CatPawAI/OpenCode |
| T-m5  | 🟢 MINOR    | 一致性   | `writeCaptchaError` 未做 JSON 转义（与 EntryPoint 不一致）         | CatPawAI         |
| T-m6  | 🟢 MINOR    | 残留     | `spring-boot-starter-session-data-redis` 依赖冗余                 | CatPawAI         |
| T-m7  | 🟢 MINOR    | 文档     | YAML 中残留 SavedRequest 注释                                     | Cline            |
| T-m8  | 🟢 MINOR    | 代码     | 手工拼接 JSON，转义不完整                                          | OpenCode/Qoder   |
| T-m9  | 🟢 MINOR    | 代码     | Admin/Portal LoginView 80% 代码重复                               | Claude/Cline/Qoder |
| T-m10 | 🟢 MINOR    | 代码     | "忘记密码"/"其他登录方式"死链接                                     | Qoder            |
| T-m11 | 🟢 MINOR    | 代码     | 前端 scope 硬编码                                                  | Qoder            |
| T-m12 | 🟢 MINOR    | 安全     | CORS `allowed-origins: "*"`（dev 配置可接受）                      | OpenCode/Qoder   |
| T-m13 | 🟢 MINOR    | 安全     | Gateway 限流阈值偏宽松                                            | Qoder            |
| T-m14 | 🟢 MINOR    | 代码     | `isTokenEndpointPost` 使用 `getRequestURI()` 而非 `getServletPath()` | OpenCode         |
| T-m15 | 🟢 MINOR    | 设计     | `LoginChannelContext` 的 ThreadLocal 传递是脆弱设计                | Qoder            |

---

## 3. 阻断性问题（CRITICAL）

### T-C1：`resolveLoginChannel` 无法获取 client_id，portal 登录完全不可用

**严重程度**：🔴 CRITICAL（功能阻断）

**影响**：所有 portal 用户（`realm=portal`）无法登录

**首次发现者**：CatPawAI / Cline / Qoder（三份报告独立发现）

**文件**：
- [OAuth2ResourceOwnerPasswordAuthenticationProvider.java#L85-L97](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/grant/password/OAuth2ResourceOwnerPasswordAuthenticationProvider.java#L85-L97)
- [OAuth2ResourceOwnerBaseAuthenticationConverter.java#L106-L110](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/grant/base/OAuth2ResourceOwnerBaseAuthenticationConverter.java#L106-L110)
- [password-grant.ts#L85-L96](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-frontend/packages/shared/src/oauth/password-grant.ts#L85-L96)

**根因分析**：

机密客户端使用 `client_secret_basic` 认证方式时，`client_id` 和 `client_secret` 通过 `Authorization: Basic base64(client_id:client_secret)` 请求头发送，**不在请求体中**。前端 `loginWithPassword()` 的请求体仅包含 `grant_type`、`username`、`password`、`scope`（及可选的验证码参数），不包含 `client_id`：

```typescript
// password-grant.ts:85-90
const body = new URLSearchParams({
    grant_type: "password",
    username,
    password,
    scope: config.scope
});
```

后端 `OAuth2ResourceOwnerBaseAuthenticationConverter.convert()` 第 107-110 行从 `request.getParameterMap()` 提取所有参数（排除 `grant_type` 和 `scope`）放入 `additionalParameters`。由于 `client_id` 不在请求体中，`additionalParameters` 中**不包含 `client_id`**。

`OAuth2ResourceOwnerPasswordAuthenticationProvider.resolveLoginChannel()` 第 92 行尝试从 `reqParameters.get("client_id")` 获取客户端 ID：

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

OAuth2ResourceOwnerBaseAuthenticationConverter.convert()
  └─ additionalParameters 不包含 client_id（getParameterMap 不读 HTTP 头）

OAuth2ResourceOwnerPasswordAuthenticationProvider.resolveLoginChannel(reqParameters)
  └─ reqParameters.get("client_id") → null
  └─ 返回 LoginChannel.ADMIN  ← 错误！应为 PORTAL

LoginChannelContext.set(ADMIN)

RoutingUserDetailsService.loadUserByUsername(username)
  └─ LoginChannelContext.get() → ADMIN
  └─ 调用 scaUserDetailsService.loadUserByUsername(username)
  └─ 查询 realm=admin 的用户  ← portal 用户在 realm=portal，查不到

结果：UsernameNotFoundException → portal 用户无法登录
```

**修复方向**：

`resolveLoginChannel` 不应从 `additionalParameters` 获取 `client_id`，而应使用已认证客户端的信息。Base Provider 的 `authenticate()` 方法中已经获取了 `registeredClient`，应将其传递给 `resolveLoginChannel`：

```java
// Base Provider 修改建议
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

### T-C2：`CaptchaVerificationFilter` 无法识别 portal 客户端，验证码校验被静默跳过

**严重程度**：🔴 CRITICAL（安全绕过）

**影响**：portal 渠道的图形验证码保护完全失效，攻击者可无限制暴力尝试密码

**首次发现者**：CatPawAI / Cline / Qoder（三份报告独立发现）

**文件**：[CaptchaVerificationFilter.java#L75-L80](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/security/filter/CaptchaVerificationFilter.java#L75-L80)

**根因分析**：

与 T-C1 同源。`CaptchaVerificationFilter` 第 76 行通过 `request.getParameter(PARAM_CLIENT_ID)` 获取客户端 ID 来判断是否为 portal 渠道：

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
2. 配合 T-C1 的修复（如果仅修复 T-C1 而不修复 T-C2），验证码仍会被跳过

**修复方向**：

方案一（推荐）：从 `SecurityContext` 获取已认证的 `OAuth2ClientAuthenticationToken`（需确保 `CaptchaVerificationFilter` 在 SAS 的 `OAuth2ClientAuthenticationFilter` 之后执行）：

```java
Authentication clientAuth = SecurityContextHolder.getContext().getAuthentication();
if (clientAuth instanceof OAuth2ClientAuthenticationToken clientToken
        && clientToken.getRegisteredClient() != null) {
    clientId = clientToken.getRegisteredClient().getClientId();
}
```

方案二：从 `Authorization` 头解析 Basic 认证获取 `client_id`（需自行 Base64 解码）。

方案三：前端在请求体中额外携带 `client_id` 参数（不推荐，违反 `client_secret_basic` 的语义，且增加参数篡改面）。

---

### T-C3：`ProviderManager` 使用 `NullEventPublisher`，登录日志/账号锁定静默失效

**严重程度**：🔴 CRITICAL（功能失效）

**影响**：
1. **登录日志完全不记录** — `LoginLogPublisher` 的 `@EventListener` 方法永远不会被触发
2. **账号锁定功能失效** — `LoginAttemptEventListener` 的 `@EventListener` 方法永远不会被触发，连续登录失败不会锁定账号
3. 两个功能都是**静默失效**（不报错、不抛异常），难以在测试中发现

**首次发现者**：CatPawAI（独有发现，其他报告均未识别）

**文件**：
- [AuthorizationServerConfig.java#L164-L168](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/config/AuthorizationServerConfig.java#L164-L168) — `authenticationManager()` 方法
- [LoginLogPublisher.java#L70-L127](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/security/LoginLogPublisher.java#L70-L127) — 依赖 `AuthenticationSuccessEvent` / `AbstractAuthenticationFailureEvent`
- [LoginAttemptEventListener.java#L27-L54](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/security/LoginAttemptEventListener.java#L27-L54) — 依赖 `AuthenticationSuccessEvent` / `AbstractAuthenticationFailureEvent`

**根因分析**：

`AuthorizationServerConfig.authenticationManager()` 第 164-168 行手动构造 `ProviderManager`：

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

LoginLogPublisher.onAuthenticationSuccess()         ← 永远不触发
LoginLogPublisher.onAuthenticationFailure()         ← 永远不触发
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
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(routingUserDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    ProviderManager providerManager = new ProviderManager(provider);
    providerManager.setAuthenticationEventPublisher(
        new DefaultAuthenticationEventPublisher(applicationEventPublisher));
    return providerManager;
}
```

**验证方法**：修复后登录一次（成功 + 失败各一次），检查 `sys_login_log` 表是否有新记录、连续失败 5 次后账号是否被锁定。

---

### T-C4：事件监听器类型检查不匹配，即使修复 T-C3 也无法触发（Trae 独有发现）

**严重程度**：🔴 CRITICAL（功能失效）

**影响**：即使修复 T-C3（注入 `DefaultAuthenticationEventPublisher`），`LoginLogPublisher` 和 `LoginAttemptEventListener` 仍然不会被触发，因为它们的类型检查在密码模式下永远不匹配

**首次发现者**：**Trae 独有发现**（其他报告均未识别此问题）

**文件**：
- [LoginLogPublisher.java#L73-L75](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/security/LoginLogPublisher.java#L73-L75) — 成功路径类型检查
- [LoginLogPublisher.java#L104-L106](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/security/LoginLogPublisher.java#L104-L106) — 失败路径类型检查
- [LoginAttemptEventListener.java#L31-L33](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/security/LoginAttemptEventListener.java#L31-L33) — 成功路径类型检查
- [LoginAttemptEventListener.java#L47-L49](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/security/LoginAttemptEventListener.java#L47-L49) — 失败路径类型检查

**根因分析**：

`LoginLogPublisher` 和 `LoginAttemptEventListener` 是为原表单登录模式设计的，类型检查基于 `UsernamePasswordAuthenticationToken` 和 `ScaUserDetails`。但在密码模式下，事件源发生了变化：

**成功路径**：
```java
// LoginLogPublisher.java:73-75
if (!(authentication.getPrincipal() instanceof ScaUserDetails details)) {
    return;  // ← 密码模式下永远 return
}
```

密码模式下，HttpSecurity 的 `ProviderManager` 调用 `OAuth2ResourceOwnerPasswordAuthenticationProvider.authenticate()`，返回 `OAuth2AccessTokenAuthenticationToken`（不是 `UsernamePasswordAuthenticationToken`）。其 principal 是 `RegisteredClient`（不是 `ScaUserDetails`）。所以 `authentication.getPrincipal() instanceof ScaUserDetails` 永远为 false。

**失败路径**：
```java
// LoginLogPublisher.java:104-106
if (!(authentication instanceof UsernamePasswordAuthenticationToken)) {
    return;  // ← 密码模式下永远 return
}
```

HttpSecurity 的 `ProviderManager` 调用 `passwordAuthenticationProvider()` 时，传入的 authentication 是 `OAuth2ResourceOwnerPasswordAuthenticationToken`（不是 `UsernamePasswordAuthenticationToken`）。所以 `authentication instanceof UsernamePasswordAuthenticationToken` 永远为 false。

**与 T-C3 的关系**：

T-C3 和 T-C4 是叠加问题：
- T-C3：内部 `ProviderManager`（手动 new 的）使用 `NullEventPublisher`，不发布事件
- T-C4：即使 HttpSecurity 的 `ProviderManager` 发布了事件（它有 eventPublisher），监听器的类型检查也会过滤掉

**这意味着仅修复 T-C3 是不够的**，还需要修复 T-C4。两者必须一并修复。

**修复方向**：

方案一（推荐）：在 `OAuth2ResourceOwnerBaseAuthenticationProvider` 内部直接发布自定义事件（不依赖 `ProviderManager` 的事件机制）：

```java
// 注入 ApplicationEventPublisher
private final ApplicationEventPublisher applicationEventPublisher;

// 在 authenticate() 方法中：
try {
    Authentication usernamePasswordAuthentication = authenticationManager.authenticate(usernamePasswordToken);
    // 直接发布成功事件（携带 ScaUserDetails）
    applicationEventPublisher.publishEvent(
        new AuthenticationSuccessEvent(usernamePasswordAuthentication));
    // ...
} catch (AuthenticationException ex) {
    // 直接发布失败事件（携带 UsernamePasswordAuthenticationToken）
    applicationEventPublisher.publishEvent(
        new AuthenticationFailureBadCredentialsEvent(
            usernamePasswordToken, ex));
    throw mapToOAuth2AuthenticationException(authentication, ex);
}
```

方案二：修改 `LoginLogPublisher` 和 `LoginAttemptEventListener` 的类型检查，使其能处理 `OAuth2AccessTokenAuthenticationToken`（成功）和 `OAuth2ResourceOwnerPasswordAuthenticationToken`（失败）。但成功路径无法获取 `ScaUserDetails`（principal 是 `RegisteredClient`），需要从其他渠道获取用户信息。

**推荐方案一**：因为它直接发布原始的 `UsernamePasswordAuthenticationToken` 和 `ScaUserDetails`，监听器无需修改类型检查。

---

## 4. 重要问题（MAJOR）

### T-M1：`client_secret` 暴露在前端 bundle 中

**严重程度**：🟡 MAJOR（安全）

**首次发现者**：6 份报告均发现

**文件**：
- [admin/.env.production#L6](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-frontend/apps/admin/.env.production#L6)
- [portal/.env.production#L6](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-frontend/apps/portal/.env.production#L6)
- [password-grant.ts#L46-L49](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-frontend/packages/shared/src/oauth/password-grant.ts#L46-L49)

**问题**：`client_secret` 通过 `VITE_OAUTH_CLIENT_SECRET` 环境变量注入前端，经 Vite 构建后明文嵌入 JS bundle。任何用户都可通过浏览器开发者工具查看 `Authorization: Basic base64(client_id:client_secret)` 头中的密钥。

**与 pig 项目的对比**：pig 项目同样存在此问题（前端硬编码 client_secret），本次改造未在此点上超越 pig。

**风险评估**：

这是密码模式 + SPA 架构的**固有妥协**（OAuth2 密码模式规范上要求机密客户端，但 SPA 天然无法保密）。用户的需求是"改造后的认证授权模式类似 pig 项目"，所以这个妥协是可以接受的。

实际风险可控：
1. 攻击者获取 `client_secret` 后可绕过 SPA 直接调用 `/oauth2/token` 端点
2. 但密码模式仍需用户名+密码，且 portal 渠道有验证码保护（T-C2 修复后）
3. admin 渠道有账号锁定保护（T-C3 修复后）

**缓解方向**（可选，不阻断合并）：
1. **短期**：为 portal 客户端配置独立的低权限 `client_secret`，与 admin 客户端隔离
2. **中期**：在 Gateway 层对 `/oauth2/token` 端点增加 IP 限流
3. **长期**：迁移到 BFF（Backend for Frontend）模式，由 BFF 持有 `client_secret`

**与 Qoder 3.1 的分歧**：Qoder 建议改回公共客户端（`client_authentication_methods: none`），但 Trae 认为这是架构决策，不应在评审中变更用户需求。用户明确要求"类似 pig 项目"，pig 项目使用机密客户端 + 密码模式。改回公共客户端虽然能解决 `client_secret` 暴露问题，但需要启用 `ScaRefreshTokenGenerator`（SAS 内置的 `OAuth2RefreshTokenGenerator` 对公共客户端返回 null），增加复杂度。

---

### T-M2：Thymeleaf 依赖未移除

**严重程度**：🟡 MAJOR（残留）

**首次发现者**：Claude / CatPawAI

**文件**：[sca-skeleton-auth/pom.xml#L69-L73](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/pom.xml#L69-L73)

**问题**：改造已删除所有 Thymeleaf 模板和静态资源，但 `spring-boot-starter-thymeleaf` 依赖仍保留在 `pom.xml` 中。Spring Boot 自动装配仍会初始化 Thymeleaf 视图解析器等组件，造成不必要的资源开销和启动时间增加。

**修复方向**：删除 `pom.xml` 中的 `spring-boot-starter-thymeleaf` 依赖。

**附注**：`pom.xml` 第 57 行注释 "OAuth 2.1 授权服务器（授权码 + PKCE 认证授权中心）" 也已过时，应改为"密码模式认证授权中心"。

---

### T-M3："记住我"复选框无实际功能

**严重程度**：🟡 MAJOR（逻辑）

**首次发现者**：5 份报告均发现（Claude/CatPawAI/Cline/OpenCode/Qoder）

**文件**：
- [admin/LoginView.vue#L19](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-frontend/apps/admin/src/views/auth/LoginView.vue#L19)
- [portal/LoginView.vue#L20](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-frontend/apps/portal/src/views/auth/LoginView.vue#L20)

**问题**：登录页保留了"记住我"复选框 UI，但：
1. `rememberMe.value` 从未被传递给登录 API 或 `authStore.login()`
2. 后端无 `RememberMeServices` 配置
3. `refresh_token` 的 TTL 由 `OAuth2ClientProperties` 统一配置，不受此选项影响

构成**误导性 UI**。

**修复方向**：要么实现差异化的 `refresh_token` TTL（勾选时延长 TTL，不勾选时缩短或不下发 `refresh_token`），要么移除复选框。建议移除（与 pig 项目一致）。

---

### T-M4：`AuthLoginProperties` 成为死代码

**严重程度**：🟡 MAJOR（残留）

**首次发现者**：CatPawAI（独有发现）

**文件**：
- [AuthLoginProperties.java](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/config/properties/AuthLoginProperties.java) — 整个类
- [AuthorizationServerConfig.java#L70](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/config/AuthorizationServerConfig.java#L70) — `@EnableConfigurationProperties` 仍引用

**问题**：`AuthLoginProperties` 原为 Thymeleaf 登录页注入配置（系统名称、Logo、版权、轮播图、表单提交 URL）。改造删除了所有 Thymeleaf 模板后，此类不再被任何代码消费，但仍在 `AuthorizationServerConfig` 的 `@EnableConfigurationProperties` 中注册。

经 Grep 验证，整个 backend 中仅 `AuthorizationServerConfig` 和 `AuthLoginProperties` 自身引用此类，无任何 getter 调用。

**修复方向**：删除 `AuthLoginProperties.java`，并从 `@EnableConfigurationProperties` 中移除引用。对应配置项 `sca.auth.login.*` 也应从 YAML 中清理（当前 YAML 中已无此配置，但 `@EnableConfigurationProperties` 仍会注册空绑定，浪费资源）。

---

### T-M5：SQL `token_settings` JSON 不完整，依赖 Initializer 自愈

**严重程度**：🟡 MAJOR（一致性）

**首次发现者**：Claude / CatPawAI / Qoder

**文件**：[sca_platform.sql#L677](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/deploy/sql/install/sca_platform.sql#L677)

**问题**：SQL 中 `token_settings` 仅包含 `{"settings.token.reuse-refresh-tokens":false}`，缺失：
- `settings.token.access-token-format`（应为 `{"value":"reference"}` 以生成不透明令牌）
- `settings.token.access-token-time-to-live`
- `settings.token.refresh-token-time-to-live`

SAS 反序列化时缺失字段使用默认值（`access-token-format` 默认为 `SELF_CONTAINED` 即 JWT），与项目硬约束「使用不透明 access token」冲突。

**当前自愈机制**：

经阅读 `OAuth2RegisteredClientInitializer` 源码确认，自愈机制确实存在但路径微妙：
1. SQL INSERT 的记录 `client_secret = NULL`
2. 启动时 `migratePublicClientIfNeeded` 第 117 行检测到 `existing.getClientSecret() == null`，判定为"公共客户端"
3. 第 125-128 行通过 `buildConfidentialClient` 重建客户端（包含完整的 token_settings 和加密的 client_secret）
4. 后续启动时 `migrateToOpaqueAccessTokenIfNeeded` 检测到 token 格式不是 REFERENCE 也会修复

**风险**：
1. 若有人在 Initializer 未运行的环境（如单元测试、手动 SQL 初始化后跳过启动）中使用该客户端，会生成 JWT 而非不透明令牌，违反安全约束
2. 自愈机制依赖 `client_secret = NULL` 被 `migratePublicClientIfNeeded` 误识别为"公共客户端"，这是一个巧合而非设计

**修复方向**：在 SQL 中补全 `token_settings` JSON：

```json
{"settings.token.access-token-format":{"value":"reference"},
 "settings.token.access-token-time-to-live":["java.time.Duration",900.000000000],
 "settings.token.refresh-token-time-to-live":["java.time.Duration",7200.000000000],
 "settings.token.reuse-refresh-tokens":false}
```

---

### T-M6：Token 吊销为 fire-and-forget

**严重程度**：🟡 MAJOR（安全）

**首次发现者**：Claude / CatPawAI

**文件**：
- [password-grant.ts#L157-L169](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-frontend/packages/shared/src/oauth/password-grant.ts#L157-L169)
- [admin/stores/auth.ts#L172-L177](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-frontend/apps/admin/src/stores/auth.ts#L172-L177)

**问题**：`revokeOAuthToken()` 使用 `try-catch` 吞掉所有异常，吊销失败时本地仍清理 token。若网络波动导致 revoke 请求未到达服务端，服务端的 access_token/refresh_token 仍然有效，用户实际未登出。

**修复方向**（可选，不阻断合并）：
1. 对 revoke 失败进行日志记录（前端 `console.warn`）
2. 在服务端设置较短的 access_token TTL（当前 15 分钟）作为兜底
3. 长期方案：在 `unload` 事件中使用 `navigator.sendBeacon()` 发送 revoke 请求

---

### T-M7：`LoginLogPublisher` 失败路径 `LoginChannelContext` 已被清理

**严重程度**：🟡 MAJOR（逻辑）

**首次发现者**：Cline（独有发现）

**文件**：
- [OAuth2ResourceOwnerBaseAuthenticationProvider.java#L192-L194](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/grant/base/OAuth2ResourceOwnerBaseAuthenticationProvider.java#L192-L194) — finally 清理 ThreadLocal
- [LoginLogPublisher.java#L200-L203](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/security/LoginLogPublisher.java#L200-L203) — 依赖 LoginChannelContext.get()

**根因分析**：

认证失败时的时序：
1. Provider.authenticate() 第 162 行设置 `LoginChannelContext.set(channel)`
2. 认证失败抛出异常
3. 第 192-194 行 `finally` 块执行 `LoginChannelContext.clear()`
4. ProviderManager 捕获异常并发布 `AbstractAuthenticationFailureEvent`
5. LoginLogPublisher.onAuthenticationFailure 第 200-203 行读取 `LoginChannelContext.get()` → null

**影响**：

登录失败日志中 `realm` 字段使用 admin 作为 fallback（`resolveRealm()` 第 201-202 行 `channel == LoginChannel.PORTAL` 为 false → 返回 "admin"），portal 渠道的失败登录会错误地以 admin realm 反查用户，找不到，tenantId/userId 留空。

**注意**：此问题与 T-C3 叠加。T-C3 修复后（事件能正常发布），T-M7 才会显现。T-C3 未修复时，事件根本不发布，T-M7 不会触发。

**修复方向**：在 `LoginLogPublisher` 中改用请求属性（`request.getAttribute(ATTR_LOGIN_CHANNEL)`）替代 ThreadLocal。Base Provider 第 367 行已将 channel 写入请求属性 `ATTR_LOGIN_CHANNEL`，请求属性在 finally 清理 ThreadLocal 后仍然可用。

---

### T-M8：`onFirstKeyFocus` 绕过 Vue 响应式直接操作 DOM

**严重程度**：🟡 MAJOR（代码）

**首次发现者**：Cline（独有发现）

**文件**：
- [admin/LoginView.vue#L147-L158](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-frontend/apps/admin/src/views/auth/LoginView.vue#L147-L158)
- portal/LoginView.vue 同名方法

**问题**：

```typescript
input.value += e.key;
input.dispatchEvent(new Event("input", { bubbles: true }));
```

直接操作 DOM 元素的 `value` 属性，然后手动派发 `input` 事件触发 Vue 的 v-model 更新。这种方式依赖 Vue 内部实现细节，版本升级后可能失效。

**修复方向**：利用 Vue 响应式机制：

```typescript
username.value = username.value + e.key;
usernameInput.value?.focus();
```

---

### T-M9：`ScaRefreshTokenGenerator` 死代码

**严重程度**：🟡 MAJOR（残留）

**首次发现者**：Qoder（独有发现）

**文件**：[ScaRefreshTokenGenerator.java](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/token/ScaRefreshTokenGenerator.java)

**问题**：`ScaRefreshTokenGenerator` 类存在完整实现（Javadoc 说明是为解决 SAS 对公共客户端不签发 refresh_token 的问题），但 [AuthorizationServerConfig.java#L209](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/config/AuthorizationServerConfig.java#L209) 实际使用的是 SAS 内置的 `OAuth2RefreshTokenGenerator`：

```java
OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();
```

**修复方向**：
- 方案一：删除 `ScaRefreshTokenGenerator.java`（如果不打算切换公共客户端）
- 方案二：在 `tokenGenerator()` 中替换为 `new ScaRefreshTokenGenerator()`（为未来切换公共客户端做准备）

建议方案一（删除），因为用户明确要求"类似 pig 项目"，pig 使用机密客户端。

---

### T-M10：用户名枚举漏洞（代码层面）

**严重程度**：🟡 MAJOR（安全）

**首次发现者**：Qoder（独有发现）

**文件**：[OAuth2ResourceOwnerBaseAuthenticationProvider.java#L329-L336](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/grant/base/OAuth2ResourceOwnerBaseAuthenticationProvider.java#L329-L336)

**问题**：

```java
if (ex instanceof UsernameNotFoundException) {
    return new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST,
            "用户 [" + authentication.getName() + "] 不存在", ERROR_URI));
}
if (ex instanceof BadCredentialsException) {
    return new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST,
            "用户名或密码错误", ERROR_URI));
}
```

"用户不存在"和"用户名或密码错误"返回不同的错误消息，攻击者可据此区分哪些用户名存在、哪些不存在（用户枚举攻击）。OWASP Authentication Cheat Sheet 明确要求：对所有认证失败返回统一的错误消息。

**实际运行时分析**：

Spring Security 的 `DaoAuthenticationProvider` 默认 `hideUserNotFoundExceptions=true`，会将 `UsernameNotFoundException` 转换为 `BadCredentialsException`。从 [AuthorizationServerConfig.java#L165](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/config/AuthorizationServerConfig.java#L165) 看，没有显式调用 `setHideUserNotFoundExceptions(false)`，所以默认为 true。

这意味着 `UsernameNotFoundException` 分支实际运行时**不会被执行**（被 `DaoAuthenticationProvider` 提前转换为 `BadCredentialsException`），最终都会返回 "用户名或密码错误"。**实际运行时不存在用户名枚举**，但代码层面的不一致仍然是潜在风险（若未来有人显式设置 `setHideUserNotFoundExceptions(false)`，漏洞会立即出现）。

**修复方向**：将 `UsernameNotFoundException` 和 `BadCredentialsException` 统一映射为 `"用户名或密码错误"`。

---

### T-M11：Token 存储在 localStorage，XSS 可窃取全部凭证

**严重程度**：🟡 MAJOR（安全）

**首次发现者**：Qoder（独有发现）

**文件**：[admin/stores/auth.ts#L117-L120](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-frontend/apps/admin/src/stores/auth.ts#L117-L120)

**问题**：access_token 和 refresh_token 均存储在 localStorage 中。任何 XSS 漏洞（包括第三方依赖引入的）都可直接读取 `localStorage` 获取全部令牌。

**风险评估**：

这是 SPA 令牌存储的常见权衡。httpOnly cookie 方案需要后端配合（Set-Cookie + CSRF 防护），改造成本较高。当前方案可接受，但应在安全评审中记录为已知风险，并确保 CSP 头配置严格。

**当前 CSP 配置**（[sca-skeleton-gateway-dev.yaml#L73](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-gateway/src/main/resources/sca-skeleton-gateway-dev.yaml#L73)）：`script-src 'self' 'unsafe-inline'` 中的 `unsafe-inline` 削弱了 CSP 对 XSS 的防护。建议长期移除 `unsafe-inline`（需要前端改造，避免内联脚本）。

**修复方向**（可选，不阻断合并）：评估 httpOnly cookie + CSRF token 方案的可行性。

---

## 5. 一般问题（MINOR）

### T-m1：多处 Javadoc 引用已删除的 `LoginChannelFilter`

**首次发现者**：5 份报告均发现

**文件**：
- [LoginLogPublisher.java#L169](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/security/LoginLogPublisher.java#L169) — `{@link LoginChannelFilter}`
- [LoginChannelContext.java#L6](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/security/LoginChannelContext.java#L6) — `{@link io.github.fushuwei.scaskeleton.auth.security.filter.LoginChannelFilter}`
- [OAuth2ResourceOwnerBaseAuthenticationProvider.java#L60](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/grant/base/OAuth2ResourceOwnerBaseAuthenticationProvider.java#L60) — `{@code LoginChannelFilter}`

**修复方向**：更新 Javadoc，将"LoginChannelFilter"改为"OAuth2ResourceOwnerBaseAuthenticationProvider"。

---

### T-m2：`resolveLoginChannel` 注释矛盾 + 死代码

**首次发现者**：Claude / CatPawAI / Cline

**文件**：[OAuth2ResourceOwnerPasswordAuthenticationProvider.java#L87-L97](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/grant/password/OAuth2ResourceOwnerPasswordAuthenticationProvider.java#L87-L97)

**问题**：
1. 注释自相矛盾：第 87 行说"附加参数中不包含 client_id"，第 90-91 行又说"实际上 client_id 会出现在 additionalParameters 中"
2. `AuthenticationProvider self = this;`（第 89 行）是死代码，从未使用

**修复方向**：删除矛盾注释和死代码。根因是 T-C1（应改用 `registeredClient.getClientId()`），修复 T-C1 后这些注释自然消失。

---

### T-m3：`@SuppressWarnings("deprecation")` 缺少说明

**首次发现者**：Claude / CatPawAI / Cline / OpenCode / Qoder

**文件**：[AuthorizationServerConfig.java#L147](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/config/AuthorizationServerConfig.java#L147)

**问题**：`passwordAuthenticationProvider()` 方法标注了 `@SuppressWarnings("deprecation")` 但未说明抑制的具体是哪个废弃 API。

**根因**：`DaoAuthenticationProvider(UserDetailsService)` 构造器在 Spring Security 6.2+ 中标记为 `@Deprecated`，推荐使用无参构造 + `setUserDetailsService()`。

**注意**：Qoder 5.3 说"该方法内部未使用任何 @Deprecated API，注解是多余的"，这是**错误的**。`DaoAuthenticationProvider(UserDetailsService)` 构造器确实已废弃。

**修复方向**：补充注释说明废弃来源，并改为推荐用法：

```java
// 移除 @SuppressWarnings("deprecation")
DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
provider.setUserDetailsService(routingUserDetailsService);
provider.setPasswordEncoder(passwordEncoder);
```

---

### T-m4：`authorization_code` 转换器冗余注册

**首次发现者**：Claude / CatPawAI / OpenCode

**文件**：[AuthorizationServerConfig.java#L124](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/config/AuthorizationServerConfig.java#L124)

**问题**：`accessTokenRequestConverter()` 注册了 `OAuth2AuthorizationCodeAuthenticationConverter`，但改造后客户端仅配置 `password` + `refresh_token` 授权类型，不再支持 `authorization_code`。此转换器永远不会匹配（请求中不会有 `grant_type=authorization_code`），属于冗余注册。

**修复方向**：可保留（作为向后兼容的基础设施），也可移除以减少不必要的转换器调用。建议保留但添加注释说明，因为 SAS 的委托机制会按顺序尝试，不影响功能。

---

### T-m5：`writeCaptchaError` 未做 JSON 转义（与 EntryPoint 不一致）

**首次发现者**：CatPawAI（独有发现）

**文件**：[CaptchaVerificationFilter.java#L124-L131](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/security/filter/CaptchaVerificationFilter.java#L124-L131)

**问题**：`writeCaptchaError()` 直接将 `message` 拼入 JSON 字符串，未做转义。而 `AuthorizationServerConfig.oauth2TokenEndpointAuthenticationEntryPoint()` 对 `message` 做了 `\\`、`\"`、`\n`、`\r`、`\t` 转义。两处生成 OAuth2 JSON 错误响应的逻辑风格不一致。

当前 `writeCaptchaError` 的 `message` 均为硬编码中文字符串（"验证码不能为空"、"验证码错误，请重新输入"），不含特殊字符，无实际注入风险。但若后续扩展为动态消息，存在 JSON 注入隐患。

**修复方向**：统一两处 JSON 生成逻辑，提取为公共工具方法并统一转义。建议使用 Jackson `ObjectMapper` 替代手工拼接。

---

### T-m6：`spring-boot-starter-session-data-redis` 依赖冗余

**首次发现者**：CatPawAI（独有发现）

**文件**：[sca-skeleton-auth/pom.xml#L63-L67](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/pom.xml#L63-L67)

**问题**：改造后认证为无状态模式（`SessionCreationPolicy.STATELESS`），不再依赖 HTTP Session。`spring-boot-starter-session-data-redis` 依赖可能冗余。

**需确认**：`RedisOAuth2AuthorizationService` 是否间接依赖 Spring Session 的 Redis 序列化机制。若 `RedisIndexedSessionRepository` 等 Session Bean 仍被其他组件引用，则不能直接移除。需运行时验证后决定。

**修复方向**：启动应用后检查是否存在 `SessionRepository` 相关 Bean；若无引用则移除依赖。同时更新注释（当前注释说"基于 Redis 存储会话"，已过时）。

---

### T-m7：YAML 中残留 SavedRequest 注释

**首次发现者**：Cline（独有发现）

**文件**：[sca-skeleton-auth-dev.yaml#L2](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/resources/sca-skeleton-auth-dev.yaml#L2)

**问题**：注释 "经网关访问时根据 X-Forwarded-* 还原对外 URL，避免登录成功后 SavedRequest 回跳到内网地址" 引用了 `SavedRequest`，这是授权码模式下的概念，密码模式下已无此逻辑。

**注意**：`forward-headers-strategy: framework` 配置本身仍然有用（用于正确解析 X-Forwarded-* 头，让 `request.getRemoteAddr()` 等返回真实客户端 IP，用于登录日志记录）。所以配置应保留，但注释应更新。

**修复方向**：更新注释为 "经网关访问时根据 X-Forwarded-* 还原对外 URL，确保登录日志记录真实客户端 IP"。

---

### T-m8：手工拼接 JSON，转义不完整

**首次发现者**：OpenCode / Qoder

**文件**：
- [AuthorizationServerConfig.java#L180-L188](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/config/AuthorizationServerConfig.java#L180-L188)
- [CaptchaVerificationFilter.java#L127-L129](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/security/filter/CaptchaVerificationFilter.java#L127-L129)

**问题**：手工转义不完整：未处理 Unicode 控制字符（如 `\u0000`–`\u001F` 中除 `\n\r\t` 外的字符）。

**修复方向**：使用 Jackson `ObjectMapper` 或 `JsonMapper` 序列化：

```java
Map<String, Object> body = Map.of(
    "error", "unauthorized",
    "error_description", message,
    "timestamp", Instant.now().toEpochMilli()
);
response.getWriter().write(new ObjectMapper().writeValueAsString(body));
```

---

### T-m9：Admin/Portal LoginView 80% 代码重复

**首次发现者**：Claude / Cline / Qoder

**文件**：
- [admin/LoginView.vue](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-frontend/apps/admin/src/views/auth/LoginView.vue)
- [portal/LoginView.vue](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-frontend/apps/portal/src/views/auth/LoginView.vue)

**问题**：两个登录页组件约 80% 的代码相同（轮播图逻辑、Toast、键盘交互、表单验证、模板结构），仅验证码部分不同。当前状态下，任何 UI 修改都需要同步改两个文件，维护成本高且容易遗漏。

**修复方向**：提取为共享 composable（`useCarousel()`、`useToast()`、`useKeyboardNavigation()`）和共享组件（`LoginFormBase.vue`，通过 slot 插入验证码区域）。

**附注**：这是代码质量建议，不阻断合并。当前重复代码可正常工作。

---

### T-m10："忘记密码"/"其他登录方式"死链接

**首次发现者**：Qoder（独有发现）

**文件**：admin/portal LoginView.vue

**问题**：

```html
<a href="#" class="md3-link md3-body-medium" tabindex="-1" @click.prevent>忘记密码？</a>
```

`@click.prevent` 阻止了默认行为但没有执行任何操作。"其他登录方式"（手机、扫码、指纹）也是纯 UI 占位。

**修复方向**：如果短期内不会实现，应添加 `disabled` 状态或 `title="即将开放"` 提示，避免用户反复点击无响应。

**附注**：这是 UI/UX 问题，不影响功能。原 Thymeleaf 登录页也是同样的占位设计，1:1 还原。

---

### T-m11：前端 scope 硬编码

**首次发现者**：Qoder（独有发现）

**文件**：[password-grant.ts#L203](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-frontend/packages/shared/src/oauth/password-grant.ts#L203)

**问题**：`scope: "profile all"` 硬编码，不从环境变量读取。修改 scope 需要改代码重新构建。

**修复方向**：增加 `VITE_OAUTH_SCOPE` 环境变量，默认值 `"profile all"`：

```typescript
scope: env.VITE_OAUTH_SCOPE ?? "profile all"
```

---

### T-m12：CORS `allowed-origins: "*"`（dev 配置可接受）

**首次发现者**：OpenCode / Qoder

**文件**：[sca-skeleton-gateway-dev.yaml#L83](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-gateway/src/main/resources/sca-skeleton-gateway-dev.yaml#L83)

**问题**：通配符允许所有来源的跨域请求。

**风险评估**：这是 dev 配置文件，注释已说明"生产环境应配置具体域名"。`allow-credentials: false` 与 `allowed-origins: "*"` 组合是安全的（浏览器规范允许）。生产环境应使用具体域名列表。

**修复方向**：在生产配置文件中使用明确的域名列表。dev 配置可保留。

---

### T-m13：Gateway 限流阈值偏宽松

**首次发现者**：Qoder（独有发现）

**文件**：[sca-skeleton-gateway-dev.yaml#L44-L45](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-gateway/src/main/resources/sca-skeleton-gateway-dev.yaml#L44-L45)

**问题**：`replenishRate: 15` 和 `burstCapacity: 30`。密码模式下 token 端点直接接受用户名密码，对暴力破解的防护偏弱。

**风险评估**：
1. 已有账号锁定机制（5 次失败锁 30 分钟）作为第二道防线（T-C3 修复后生效）
2. 限流是按 IP（`userKeyResolver`）而非按用户名
3. 这是 dev 配置，生产环境可以调整

**修复方向**：生产环境收紧到 5/10 或按 IP+用户名组合限流。

---

### T-m14：`isTokenEndpointPost` 使用 `getRequestURI()` 而非 `getServletPath()`

**首次发现者**：OpenCode（独有发现）

**文件**：[CaptchaVerificationFilter.java#L109-L110](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/security/filter/CaptchaVerificationFilter.java#L109-L110)

**问题**：`getRequestURI()` 返回完整 URI（含 context-path），如果 auth 服务配置了 context-path（如 `/auth`），则 `request.getRequestURI()` 会返回 `/auth/oauth2/token`，与 `TOKEN_URI`（`/oauth2/token`）不匹配。

**实际影响**：从配置看 auth 服务没有配置 context-path（由网关 `StripPrefix=1` 转发），所以当前实现是正确的。但 `getServletPath()` 是更标准的做法。

**修复方向**：改为 `getServletPath()` 以提高可移植性。

---

### T-m15：`LoginChannelContext` 的 ThreadLocal 传递是脆弱设计

**首次发现者**：Qoder（独有发现）

**文件**：[LoginChannelContext.java](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/security/LoginChannelContext.java)

**问题**：当前 `LoginAttemptEventListener` 和 `LoginLogPublisher` 是同步 `@EventListener`，ThreadLocal 可用。但如果未来有人加上 `@Async` 或使用 `ApplicationEventMulticaster` 的异步模式，ThreadLocal 立即失效。

**修复方向**：在事件对象中显式携带 channel 信息，而非依赖 ThreadLocal 隐式传递。Base Provider 第 367 行已将 channel 写入请求属性 `ATTR_LOGIN_CHANNEL`，可作为备选方案。

**附注**：这是设计层面的潜在风险，当前不影响功能。与 T-M7 相关。

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

### 6.7 前端 refresh token 并发控制

`axios-oauth.ts` 的 `refreshPromise` 单例锁确保并发 401 时只触发一次 refresh，避免 refresh_token 被多次消费（`reuseRefreshTokens=false` 下多次消费会导致后续 refresh 失败）。pig 项目没有这个机制。

### 6.8 令牌脱敏日志

`OAuth2ResourceOwnerBaseAuthenticationProvider.maskToken()` 方法（[第 374-379 行](file:///Users/fuwei/Documents/Workspace/work/sca-skeleton/sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/grant/base/OAuth2ResourceOwnerBaseAuthenticationProvider.java#L374-L379)）仅展示令牌前 8 位，避免敏感信息泄露到日志。pig 项目没有这个机制。

---

## 7. 与已有 QA 报告的交叉验证

### 7.1 关键发现归属

| 问题                               | catpawai | claude | cline | opencode | qoder | windsurf | **trae** |
|------------------------------------|:--------:|:------:|:-----:|:--------:|:-----:|:--------:|:--------:|
| T-C1 portal 登录不可用             |    ✅    |   ❌   |  ✅   |    ❌    |  ✅   |    ❌    |    ✅    |
| T-C2 验证码静默跳过                |    ✅    |   ❌   |  ✅   |    ❌    |  ✅   |    ❌    |    ✅    |
| T-C3 事件发布失效                  |    ✅    |   ❌   |  ❌   |    ❌    |  ❌   |    ❌    |    ✅    |
| **T-C4 监听器类型检查不匹配**      |    ❌    |   ❌   |  ❌   |    ❌    |  ❌   |    ❌    |    ✅    |
| T-M1 client_secret 暴露            |    ✅    |   ✅   |  ✅   |    ✅    |  ✅   |    ✅    |    ✅    |
| T-M2 Thymeleaf 残留                |    ✅    |   ✅   |  ❌   |    ❌    |  ❌   |    ❌    |    ✅    |
| T-M3 "记住我"无效                  |    ✅    |   ✅   |  ✅   |    ✅    |  ✅   |    ❌    |    ✅    |
| T-M4 AuthLoginProperties 死代码    |    ✅    |   ❌   |  ❌   |    ❌    |  ❌   |    ❌    |    ✅    |
| T-M5 SQL token_settings 不完整     |    ✅    |   ✅   |  ❌   |    ❌    |  ✅   |    ❌    |    ✅    |
| T-M6 revoke fire-and-forget        |    ✅    |   ✅   |  ❌   |    ❌    |  ❌   |    ❌    |    ✅    |
| T-M7 失败日志渠道字段不准确        |    ❌    |   ❌   |  ✅   |    ❌    |  ❌   |    ❌    |    ✅    |
| T-M8 直接操作 DOM                  |    ❌    |   ❌   |  ✅   |    ❌    |  ❌   |    ❌    |    ✅    |
| T-M9 ScaRefreshTokenGenerator 死代码 |    ❌    |   ❌   |  ❌   |    ❌    |  ✅   |    ❌    |    ✅    |
| T-M10 用户名枚举                   |    ❌    |   ❌   |  ❌   |    ❌    |  ✅   |    ❌    |    ✅    |
| T-M11 Token 存 localStorage        |    ❌    |   ❌   |  ❌   |    ❌    |  ✅   |    ❌    |    ✅    |

### 7.2 错误的发现（其他报告）

| 错误发现                                          | 来源         | 实际情况                                                                                    |
|--------------------------------------------------|--------------|---------------------------------------------------------------------------------------------|
| "E2E 测试 localStorage key 与生产代码不匹配"      | Cline m-5 / OpenCode 问题11 | **错误**。测试代码 `admin_access_token` / `admin_refresh_token` 与 `auth-storage.ts` 常量值完全匹配 |
| "该方法内部未使用任何 @Deprecated API，注解是多余的" | Qoder 5.3    | **错误**。`DaoAuthenticationProvider(UserDetailsService)` 构造器在 Spring Security 6.2+ 中标记为 `@Deprecated` |
| "LoginPageController 被删除但无替换说明"          | Claude ISSUE-10 | **部分错误**。旧 URL 重定向不是必须的，用户可以使用新的 SPA URL。但添加重定向 Controller 是合理的兼容性建议 |

### 7.3 Trae 独有发现

- **T-C4**：事件监听器类型检查不匹配（即使修复 T-C3 也无法触发）— 这是本次评审最重要的独有发现，揭示了仅修复 T-C3 是不够的

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
- 前端 refresh token 并发控制
- 令牌脱敏日志

但存在 **4 个阻断性缺陷**导致核心功能不可用，**必须在合并前修复**：

| 编号 | 问题                              | 影响                              | 修复难度                                                |
|------|-----------------------------------|-----------------------------------|---------------------------------------------------------|
| T-C1 | `resolveLoginChannel` 无法获取 client_id | portal 登录完全不可用             | 低（改用 `registeredClient.getClientId()`）             |
| T-C2 | `CaptchaVerificationFilter` 无法识别 portal | 验证码校验静默跳过                | 低（从 SecurityContext 或 Authorization 头获取）        |
| T-C3 | `ProviderManager` 未注入事件发布器 | 登录日志 + 账号锁定静默失效       | 低（注入 `DefaultAuthenticationEventPublisher`）        |
| T-C4 | 事件监听器类型检查不匹配          | 即使修复 T-C3 也无法触发          | 中（需在 Provider 内部直接发布事件，或修改监听器类型检查） |

### 修复优先级

1. **P0（阻断合并）**：T-C1 → T-C2 → T-C3 + T-C4（四者关联，建议一并修复）
   - T-C1 和 T-C2 同源（client_id 获取方式错误），修复方案应统一
   - T-C3 和 T-C4 叠加（事件发布 + 类型检查），必须一并修复
2. **P1（合并后尽快）**：T-M2（Thymeleaf 残留）、T-M4（死代码清理）、T-M5（SQL 补全）、T-M9（ScaRefreshTokenGenerator 处理）
3. **P2（后续迭代）**：T-M1（client_secret 架构性妥协）、T-M3（记住我）、T-M6（revoke 健壮性）、T-M7（失败日志渠道）、T-M10（用户名枚举防御）
4. **P3（代码洁癖）**：T-M8、T-M11、T-m1 ~ T-m15（文档与代码一致性、代码质量）

### 验证清单

修复 T-C1/T-C2/T-C3/T-C4 后，需执行以下验证：

- [ ] admin 渠道登录成功，`sys_login_log` 有成功记录
- [ ] admin 渠道登录失败（错误密码），`sys_login_log` 有失败记录，连续 5 次后账号锁定
- [ ] portal 渠道登录成功（含验证码），`sys_login_log` 有成功记录
- [ ] portal 渠道登录不填验证码 → 返回"验证码不能为空"
- [ ] portal 渠道登录填错验证码 → 返回"验证码错误，请重新输入"
- [ ] portal 渠道连续登录失败 5 次后账号锁定
- [ ] 令牌刷新（refresh_token）正常工作
- [ ] 退出登录（revoke）后 access_token 失效
- [ ] 现有功能回归：租户管理、用户管理、角色管理、菜单管理、操作日志正常

### 与 GLM 总结的对比

GLM 的改造总结整体准确，但有以下遗漏：
1. 未提及 T-C1/T-C2/T-C3/T-C4 四个阻断性问题
2. 未提及 T-M4（AuthLoginProperties 死代码）、T-M9（ScaRefreshTokenGenerator 死代码）
3. 未提及 T-M7（失败日志渠道字段不准确）
4. 未提及 T-M10（用户名枚举漏洞）

这些遗漏主要因为 GLM 未进行运行时链路推演，仅从代码结构层面总结。建议 GLM 在后续改造中加入链路推演环节。

---

> **评审声明**：本报告基于对 `origin/1.0.0` vs `main` 全部 91 个变更文件的逐行静态审查 + 运行时链路推演，未执行运行时验证。T-C1/T-C2 的根因分析基于 OAuth2 规范（RFC 6749）和 Servlet API 规范；T-C3 的根因分析基于 Spring Security 6 源码（`ProviderManager` 默认使用 `NullEventPublisher`）；T-C4 的根因分析基于对 `LoginLogPublisher` / `LoginAttemptEventListener` 类型检查与密码模式下事件源类型的对比。建议修复后按上述验证清单进行实际运行验证。
