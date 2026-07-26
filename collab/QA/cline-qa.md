# CLINE QA 评审报告：授权码+PKCE → 密码模式改造

> **评审日期：** 2026-07-26
> **评审版本：** commit `5dc0d434`
> **评审范围：** 全栈（后端 Java、前端 TypeScript/Vue、SQL、网关配置、E2E 测试）
> **评审结论：** ⚠️ 有条件通过（存在 3 个 Critical 级别问题必须修复）

---

## 1. 总体评价

本次改造从**授权码+PKCE 模式**迁移至**密码模式（Resource Owner Password Credentials Grant）**，整体代码结构清晰、分层合理，体现了较好的工程素养。后端基于 Spring Authorization Server 扩展了自定义的 `password` grant_type，采用模板方法模式设计了可扩展的「Base」抽象层，为未来扩展短信验证码等其他自定义授权类型预留了良好的扩展点。前端将登录页从后端 Thymeleaf 迁移至 SPA 组件，轮播图、Material Design 3 样式均保持了视觉一致性。

**然而，代码中存在 3 个 Critical 级别的功能/安全缺陷**，若不修复将导致线上系统无法正常运行或产生严重安全风险：

- **C-1：客户端密钥硬编码在前端构建产物中**——机密客户端的意义完全丧失
- **C-2：CaptchaVerificationFilter 无法正确读取 `client_id`**——portal 渠道验证码校验永远跳过
- **C-3：登录渠道（LoginChannel）解析失效**——portal 用户被错误路由到 admin realm

---

## 2. 评审分级标准

| 等级 | 定义 | 必须处理 |
|------|------|----------|
| **Critical** | 功能阻断、安全漏洞、数据丢失风险 | 上线前必须修复 |
| **Major** | 非阻断但严重的设计/规范/异常处理缺陷 | 建议上线前修复 |
| **Minor** | 代码质量、文档注释、可读性等问题 | 可选修复 |
| **Info** | 改进建议、最佳实践推荐 | 参考 |

---

## 3. Critical 级别问题

### C-1：客户端密钥暴露在前端构建产物中

**涉及文件：** `apps/admin/.env.production` 第 6 行、`apps/portal/.env.production` 第 5 行

**问题描述：**
```env
VITE_OAUTH_CLIENT_SECRET=admin-secret
VITE_OAUTH_CLIENT_SECRET=portal-secret
```

客户端密钥（`client_secret`）以明文形式配置在 Vite 环境变量中，构建后将打包到前端 JS 产物中。任何用户通过浏览器 DevTools 即可提取该密钥。**这意味着「机密客户端」的机密性完全丧失**——攻击者可获取密钥后模拟客户端发起令牌请求。

**影响分析：**
- 机密客户端的核心安全假设（`client_secret` 仅服务端知晓）被打破
- 攻击者可获取密钥后构造任意令牌请求
- 速率限制和客户端级别的安全控制失效

**建议修复方案：**
1. **推荐方案：** 在网关层（Gateway）或 BFF 层注入 `client_secret`，前端完全不接触密钥。网关在转发 `/auth/oauth2/token` 请求时自动附加 Basic Auth 头。
2. **备选方案：** 若短期内无法引入 BFF，至少使用运行时环境变量注入而非硬编码，且严格限制密钥的权限范围。

---

### C-2：CaptchaVerificationFilter 无法读取 client_id（portal 验证码失效）

**涉及文件：** 
- `packages/shared/src/oauth/password-grant.ts` 第 85-96 行
- `CaptchaVerificationFilter.java` 第 76-80 行

**问题描述：**

前端 `loginWithPassword()` 发送的请求体为 `grant_type=password&username=xxx&password=xxx&scope=profile+all`，**`client_id` 不包含在请求体中**，而是通过 `Authorization: Basic base64(client_id:client_secret)` 头部传递。

后端 `CaptchaVerificationFilter` 的 `isPortalClient()` 方法通过 `request.getParameter(PARAM_CLIENT_ID)` 读取 `client_id`——该方法**仅读取 HTTP Body/Query 参数**，无法读取 Authorization 头部中的 client_id。因此 `isPortalClient(null)` 始终返回 `false`。

**影响分析：**
- portal 渠道的图形验证码校验**永久跳过**，前端登录页的验证码输入框形同虚设
- 攻击者可对 portal 登录接口进行暴力密码猜测攻击

**建议修复方案：**
方案 A（推荐）：前端在 token 请求体中显式传递 `client_id` 参数：
```typescript
const body = new URLSearchParams({
  grant_type: "password",
  client_id: config.clientId,   // ← 新增
  username,
  password,
  scope: config.scope
});
```
方案 B：`CaptchaVerificationFilter` 从 `SecurityContext` 读取已认证的客户端主体获取 client_id。

---

### C-3：登录渠道（LoginChannel）解析失效，portal 用户无法登录

**涉及文件：**
- `OAuth2ResourceOwnerPasswordAuthenticationProvider.java` 第 86-96 行
- `RoutingUserDetailsService.java` 第 26-34 行

**问题描述：**

`resolveLoginChannel()` 从 `additionalParameters`（来自请求体参数）中读取 `client_id`：

```java
Object clientId = reqParameters.get("client_id");
if (clientId != null && ...) { return LoginChannel.PORTAL; }
return LoginChannel.ADMIN;
```

由于前端请求体不包含 `client_id`（通过 Basic Auth 头发送），此方法**始终返回 `LoginChannel.ADMIN`**。方法注释也存在自相矛盾（第 87-91 行）。

**影响分析：**
- portal 渠道的 `RoutingUserDetailsService` 使用 `realm=admin` 查询用户
- portal 用户（`realm=portal`）将因找不到用户而登录失败
- **portal 渠道完全不可用**

**建议修复方案：**
方案 A：通过已认证的客户端主体获取 client_id（与 C-2 方案 B 统一）：
```java
public LoginChannel resolveLoginChannel(Map<String, Object> reqParameters) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof OAuth2ClientAuthenticationToken clientAuth) {
        RegisteredClient client = clientAuth.getRegisteredClient();
        if (client != null && oauth2ClientProperties.getPortal().getClientId().equals(client.getClientId())) {
            return LoginChannel.PORTAL;
        }
    }
    return LoginChannel.ADMIN;
}
```
方案 B：前端在请求体中添加 `client_id`（与 C-2 方案 A 统一）。

---

## 4. Major 级别问题

### M-1：onFirstKeyFocus 绕过 Vue 响应式系统直接操作 DOM

**涉及文件：** `admin/src/views/auth/LoginView.vue` 第 147-158 行、portal 同名文件类似实现

**问题描述：**
```typescript
input.value += e.key;
input.dispatchEvent(new Event("input", { bubbles: true }));
```
直接操作 DOM 元素的 `value` 属性，然后手动派发 `input` 事件触发 Vue 的 v-model 更新。这种方式依赖 Vue 内部实现细节，版本升级后可能失效。手动管理事件监听器也增加了维护负担。

**建议修复方案：**
利用 Vue 响应式机制，通过 ref 更新数据：
```typescript
username.value = username.value + e.key;
usernameInput.value?.focus();
```

---

### M-2：rememberMe 复选框无实际功能

**涉及文件：** `admin/src/views/auth/LoginView.vue` 第 305 行、portal 同名文件第 390 行

**问题描述：**
模板中渲染了「记住我」复选框并双向绑定到 `rememberMe` ref，但该变量在 `handleLogin()` 及任何逻辑中均未被使用。勾选后不产生任何实际效果，属于**幻影功能**。

**建议修复方案：**
- 若计划实现：登录成功后根据 `rememberMe` 决定 refresh_token 是否持久化到 localStorage
- 若不计划实现：从模板中移除该复选框

---

### M-3：LoginLogPublisher 在认证失败时 LoginChannelContext 已为空

**涉及文件：**
- `OAuth2ResourceOwnerBaseAuthenticationProvider.java` 第 192-195 行（finally 清理 ThreadLocal）
- `LoginLogPublisher.java` 第 200-203 行（依赖 LoginChannelContext.get()）

**问题描述：**
认证失败时的时序：
1. Provider.authenticate() 设置 `LoginChannelContext.set(channel)`（第 162 行）
2. 认证失败抛出异常
3. `finally` 块执行 `LoginChannelContext.clear()`（第 194 行）
4. ProviderManager 捕获异常并同步发布 `AbstractAuthenticationFailureEvent`
5. LoginLogPublisher.onAuthenticationFailure 读取 `LoginChannelContext.get()` → null

**影响分析：**
登录失败日志中 `realm` 字段使用 admin 作为 fallback，portal 渠道的登录失败审计数据不准确。

**建议修复方案：**
在 `LoginLogPublisher` 中改用请求属性（`request.getAttribute(ATTR_LOGIN_CHANNEL)`）替代 ThreadLocal。请求属性在 finally 清理 ThreadLocal 后仍然可用。

---

### M-4：DaoAuthenticationProvider 使用废弃构造方法

**涉及文件：** `AuthorizationServerConfig.java` 第 147 行

```java
@SuppressWarnings("deprecation")
```

`DaoAuthenticationProvider(UserDetailsService)` 在 Spring Security 6.x 中已标记为废弃，推荐使用无参构造 + `setUserDetailsService()`：

```java
DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
provider.setUserDetailsService(routingUserDetailsService);
provider.setPasswordEncoder(passwordEncoder);
```

---

## 5. Minor 级别问题

### m-1：resolveLoginChannel 注释自相矛盾

**涉及文件：** `OAuth2ResourceOwnerPasswordAuthenticationProvider.java` 第 86-92 行

```
// client_id 不在 additionalParameters 中（被 Converter 过滤为非标准参数保留）
// 实际上 client_id 会出现在 additionalParameters 中（因为它不是 grant_type 或 scope）
```

两条注释相互矛盾，反映代码逻辑的不确定性。

---

### m-2：Converter 中 checkParams 方法命名语义模糊

**涉及文件：** `OAuth2ResourceOwnerBaseAuthenticationConverter.java` 第 56 行

`checkParams` 建议改为 `validateAdditionalParameters` 以更清晰表达职责。

---

### m-3：YAML 中残留 SavedRequest 相关注释

**涉及文件：** `sca-skeleton-auth-dev.yaml` 第 3 行

```yaml
# 经网关访问时根据 X-Forwarded-* 还原对外 URL，避免登录成功后 SavedRequest 回跳到内网地址
```

密码模式下已无 SavedRequest 重定向逻辑，该注释已过时。

---

### m-4：登录页硬编码中文文案

**涉及文件：** admin/portal LoginView.vue

Toast 组件内的文案通过参数传入，但存在较多硬编码中文占位符。若项目已有 i18n 方案请接入，否则确认无国际化需求后保留现状。

---

### m-5：E2E 测试使用的 localStorage key 与生产代码不匹配

**涉及文件：** `e2e/session-timeout-redirect.spec.ts` 第 24-25 行

测试代码使用 `admin_access_token` 和 `admin_refresh_token`，但生产代码使用的 key 来自 `auth-storage.ts` 常量（如 `admin_token`、`admin_refresh_token`）。**测试步骤 2「模拟会话过期」实际未能清除正确的 key**，导致测试断言可能因环境差异而通过但逻辑无效。

---

## 6. Info 级别建议

### i-1：密码模式的 OAuth 2.1 兼容性说明

`OAuth2GrantTypeConstants.java` 已通过 Javadoc 说明密码模式在 OAuth 2.1 中已废弃。建议在项目 README 或架构文档中补充采用密码模式的原因，并考虑未来向 authorization_code + PKCE 演进的中长期规划。

### i-2：Token 端点 JSON 错误响应手动拼接存在转义风险

`AuthorizationServerConfig.java` 第 178-188 行手动拼接 JSON 字符串。建议使用 Jackson ObjectMapper 或 Spring 的 `ProblemDetail` API：
```java
Map<String, Object> body = Map.of(
    "error", "unauthorized",
    "error_description", message,
    "timestamp", Instant.now().toEpochMilli()
);
response.getWriter().write(new ObjectMapper().writeValueAsString(body));
```

### i-3：Portal 登录页验证码图片路径约定

前通过 `/auth/captcha/generate?key=xxx` 获取验证码图片 Base64 JSON。该路径依赖网关白名单放行。建议在环境配置中规范化该路径使其可配置。

### i-4：登录页组件化提取

Admin 和 Portal 的 LoginView.vue 约 80%+ 代码重复（轮播图、表单验证、Toast、键盘交互等），建议提取为共享组件。


---

## 7. 文件级逐项评审

### 7.1 后端新增/修改文件

#### `OAuth2GrantTypeConstants.java` ✅ 良好
- 使用 `AuthorizationGrantType` 而非字符串常量
- Javadoc 说明 OAuth 2.1 废弃状态
- 私有构造器防止实例化

#### `OAuth2ResourceOwnerBaseAuthenticationToken.java` ✅ 良好
- 模板方法模式应用得当
- `Collections.unmodifiableSet/unmodifiableMap` 确保不可变性
- 防御性拷贝防止外部修改

#### `OAuth2ResourceOwnerBaseAuthenticationConverter.java` ✅ 良好
- 不依赖 SAS 内部 `OAuth2EndpointUtils`，避免版本兼容问题
- 清晰的步骤注释
- 使用 `getParameterMap()` 提取所有参数

#### `OAuth2ResourceOwnerBaseAuthenticationProvider.java` ✅ 整体良好（M-3）
- `try/catch/finally` 正确清理 ThreadLocal
- 异常映射覆盖 7 种常见 AuthenticationException
- 令牌生成遵循 SAS 标准上下文模式

#### `OAuth2ResourceOwnerPasswordAuthenticationToken.java` ✅ 良好

#### `OAuth2ResourceOwnerPasswordAuthenticationConverter.java` ✅ 良好
- 参数校验完善，检查重复参数
- 日志在 warning 级别记录校验失败

#### `OAuth2ResourceOwnerPasswordAuthenticationProvider.java` ⚠️ C-3
- `resolveLoginChannel` 读取 client_id 方式有缺陷
- 注释自相矛盾（m-1）

#### `AuthorizationServerConfig.java` ✅ 整体良好
- Converter 委托模式设计合理
- CaptchaVerificationFilter 正确插入位置

#### `AuthSecurityConfig.java` ✅ 良好

#### `CaptchaVerificationFilter.java` ⚠️ C-2
- `isPortalClient()` 依赖 body 参数读取 client_id
- 使用 Basic Auth 时始终 false

#### `OAuth2RegisteredClientInitializer.java` ✅ 良好
- 优雅处理 JSON 格式不兼容
- 支持令牌格式在线迁移

#### `OAuth2ClientProperties.java` ✅ 良好

#### `ScaOpaqueAccessTokenClaimsCustomizer.java` ✅ 良好

#### `LoginLogPublisher.java` ✅ 良好

### 7.2 前端新增/修改文件

#### `password-grant.ts` ✅ 整体良好（C-2 关联）

#### `axios-oauth.ts` ✅ 良好

#### `admin/src/stores/auth.ts` ✅ 良好

#### `admin/src/router/guards.ts` ✅ 良好

#### `admin/portal LoginView.vue` ✅ 整体良好

#### `login.scss` ✅ 良好

### 7.3 配置变更

#### `sca-skeleton-gateway-dev.yaml` ✅ 良好

#### `sca-skeleton-auth-dev.yaml` ✅ 良好

#### `deploy/sql/install/sca_platform.sql` ✅ 良好

---

## 8. 改造完整性检查清单

| 检查项 | 状态 | 备注 |
|--------|------|------|
| 授权码模式代码完全移除 | ✅ | 所有相关类已删除 |
| 密码模式 grant 扩展实现 | ✅ | Base + Password 体系 |
| SAS 原生 authorization_code 兼容 | ✅ | Converter 列表中保留 |
| Token 端点路由白名单 | ✅ | 网关配置正确 |
| 登录页迁移到 SPA | ✅ | 样式一致 |
| 密码可见性切换 | ✅ | |
| 验证码（portal 渠道） | ⚠️ | **C-2：验证码校验不可用** |
| 令牌刷新 | ✅ | |
| 令牌吊销 | ✅ | |
| 静默 refresh token 续期 | ✅ | |
| 登录审计日志 | ⚠️ | **M-3：失败日志渠道字段不准确** |
| 渠道路由（admin/portal） | ❌ | **C-3：portal 用户无法登录** |
| E2E 测试有效性 | ⚠️ | **m-5：localStorage key 不匹配** |
| client_secret 安全性 | ❌ | **C-1：密钥暴露在前端** |
| 现有业务功能不受影响 | ⚠️ | 核心认证流程存在问题 |

---

## 9. 总结

### 9.1 做得好的方面

1. **扩展性设计**：Base 抽象层为未来扩展提供了清晰的模板方法模式，优于 pig 项目中 Hutool/SpringUtil 的静态工具类依赖。

2. **ThreadLocal 管理**：`finally` 块中清理 `LoginChannelContext`，防止线程池复用导致渠道串扰。

3. **前端登录页 1:1 还原**：从 MD3 CSS 变量到轮播图动效、Tab 循环、Toast 动画、首次按键聚焦，完整保留了原 Thymeleaf 页面的视觉和交互体验。

4. **令牌刷新并发控制**：`refreshAccessTokenOnce` 使用 Promise 缓存机制，避免多个 401 并发触发多次 refresh。

5. **OAuth2 错误映射**：完整覆盖 7 种 Spring Security 认证异常的映射，返回标准 OAuth2 JSON 错误格式。

### 9.2 必须修复的问题（按优先级）

| 优先级 | ID | 问题 | 影响 |
|--------|----|------|------|
| 🔴 P0 | C-3 | 登录渠道解析失败 | portal 渠道完全不可用 |
| 🔴 P0 | C-2 | 验证码校验永久跳过 | portal 登录安全防线缺失 |
| 🔴 P0 | C-1 | 客户端密钥暴露 | 机密客户端形同虚设 |
| 🟠 P1 | M-3 | 失败日志渠道字段不准确 | 审计数据不准确 |
| 🟠 P1 | M-2 | 记住我功能未实现 | 用户困惑 |
| 🟡 P2 | M-1 | 直接操作 DOM | 维护性隐患 |
| 🟡 P2 | m-5 | E2E 测试 key 不匹配 | 测试有效性存疑 |

### 9.3 最终结论

本次改造的架构设计、代码结构和功能覆盖度整体质量较高，**但 3 个 Critical 级别的缺陷导致核心认证流程的不可用和安全基线的缺失**：
- **C-3（P0）**：portal 用户完全无法登录——功能阻断
- **C-2（P0）**：验证码防线永久失效——安全漏洞
- **C-1（P0）**：client_secret 完全暴露——安全漏洞

建议按照以上优先级修复后重新评审，在确认 C-1/C-2/C-3 修复完成后方可上线。

---

*评审报告结束*

