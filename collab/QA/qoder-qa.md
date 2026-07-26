# 密码模式改造代码评审报告

> 评审人：QoderWork  
> 评审日期：2026-07-26  
> 评审范围：OAuth2 授权码+PKCE → 密码模式（SAS 扩展）全链路改造  
> 涉及模块：sca-skeleton-auth、sca-skeleton-gateway、sca-skeleton-frontend（shared / admin / portal）、deploy/sql

---

## 一、总体评价

本次改造在架构层面完成了从授权码+PKCE 到密码模式的切换，后端基于 Spring Authorization Server 扩展了自定义 `password` grant type，前端 SPA 接管了登录页渲染。整体代码结构清晰，分层合理（Converter → Provider → Token 三层抽象），Javadoc 覆盖率高，日志埋点充分。

但评审发现了 **2 个阻断性功能缺陷**（portal 渠道完全不可用）、**1 个严重安全设计问题**（client_secret 前端暴露）以及若干中低优先级问题。以下按严重程度分级列出。

---

## 二、阻断性问题（P0 — 必须修复，否则功能不可用）

### 2.1 Portal 渠道 LoginChannel 判定失效，portal 用户无法登录

**文件：** `sca-skeleton-auth/.../grant/password/OAuth2ResourceOwnerPasswordAuthenticationProvider.java`，`resolveLoginChannel()` 方法（约第 846–857 行）

**问题描述：**

```java
@Override
public LoginChannel resolveLoginChannel(Map<String, Object> reqParameters) {
    Object clientId = reqParameters.get("client_id");
    if (clientId != null && oauth2ClientProperties.getPortal().getClientId().equals(clientId)) {
        return LoginChannel.PORTAL;
    }
    return LoginChannel.ADMIN;
}
```

该方法从 `reqParameters`（即 Converter 传递的 additionalParameters）中读取 `client_id`。然而前端使用 `client_secret_basic` 认证方式，`client_id` 通过 HTTP `Authorization: Basic base64(client_id:client_secret)` 头传递，**请求体中不包含 `client_id` 参数**。

前端 `password-grant.ts` 的 `loginWithPassword()` 构造的请求体：

```typescript
const body = new URLSearchParams({
  grant_type: "password",
  username,
  password,
  scope: config.scope
});
```

因此 `reqParameters.get("client_id")` 始终返回 `null`，`resolveLoginChannel()` 始终返回 `LoginChannel.ADMIN`。

**影响链路：**

1. Portal 用户登录 → LoginChannel 被设为 ADMIN
2. `RoutingUserDetailsService.loadUserByUsername()` 走 admin 分支 → 查询 `realm='admin'`
3. Portal 用户（`realm='portal'`）查不到 → 抛出 `UsernameNotFoundException`
4. **Portal 渠道登录功能完全不可用**

**修复建议：**

从 `SecurityContextHolder` 获取已认证的客户端信息（此时 SAS 的 `OAuth2ClientAuthenticationFilter` 已完成客户端认证）：

```java
@Override
public LoginChannel resolveLoginChannel(Map<String, Object> reqParameters) {
    Authentication clientAuth = SecurityContextHolder.getContext().getAuthentication();
    if (clientAuth instanceof OAuth2ClientAuthenticationToken clientToken
            && clientToken.getRegisteredClient() != null) {
        String clientId = clientToken.getRegisteredClient().getClientId();
        if (oauth2ClientProperties.getPortal().getClientId().equals(clientId)) {
            return LoginChannel.PORTAL;
        }
    }
    return LoginChannel.ADMIN;
}
```

**附加问题：** 该方法中有两行自相矛盾的注释和一个无用语句 `AuthenticationProvider self = this;`，应一并清理。

---

### 2.2 CaptchaVerificationFilter 无法识别 Portal 渠道，验证码校验被跳过

**文件：** `sca-skeleton-auth/.../security/filter/CaptchaVerificationFilter.java`，`doFilterInternal()` 方法（约第 1274 行）

**问题描述：**

```java
String clientId = request.getParameter(PARAM_CLIENT_ID);
if (!isPortalClient(clientId)) {
    filterChain.doFilter(request, response);
    return;
}
```

与 2.1 同源：`client_secret_basic` 模式下 `client_id` 在 Authorization 头中，`request.getParameter("client_id")` 返回 `null`，`isPortalClient(null)` 返回 `false`，验证码校验逻辑被完全跳过。

**影响：** Portal 渠道的图形验证码形同虚设，攻击者可绕过验证码直接暴力破解密码。

**修复建议：**

方案 A（推荐）：从 SecurityContext 获取已认证客户端（需确保 CaptchaVerificationFilter 在 SAS 的 `OAuth2ClientAuthenticationFilter` 之后执行）：

```java
Authentication clientAuth = SecurityContextHolder.getContext().getAuthentication();
if (clientAuth instanceof OAuth2ClientAuthenticationToken clientToken
        && clientToken.getRegisteredClient() != null) {
    clientId = clientToken.getRegisteredClient().getClientId();
}
```

方案 B：从 Authorization 头解析 Basic 认证中的 client_id（需自行 Base64 解码）。

方案 C：前端在请求体中额外携带 `client_id` 参数（不推荐，违反 `client_secret_basic` 的语义，且增加了参数篡改面）。

---

## 三、严重安全问题（P1）

### 3.1 client_secret 硬编码在前端环境变量中，构建后公开可见

**文件：**
- `apps/admin/.env.development`：`VITE_OAUTH_CLIENT_SECRET=admin-secret`
- `apps/admin/.env.production`：`VITE_OAUTH_CLIENT_SECRET=admin-secret`
- `apps/portal/.env.development`：`VITE_OAUTH_CLIENT_SECRET=portal-secret`
- `apps/portal/.env.production`：`VITE_OAUTH_CLIENT_SECRET=portal-secret`

**问题描述：**

Vite 构建时会将所有 `VITE_` 前缀的环境变量内联到 JavaScript bundle 中。任何人打开浏览器 DevTools → Sources 面板即可看到完整的 `client_id:client_secret`。

**RFC 6749 §2.1 第一性原理分析：**

> "Clients capable of maintaining the confidentiality of their credentials (e.g., client implemented on a secure server with restricted access to the client credentials), or capable of secure client authentication using other means."

SPA 运行在用户浏览器中，无法限制对 client_secret 的访问。将 SPA 配置为"机密客户端"（`client_secret_basic`）并在前端代码中携带 secret，是一种**安全幻觉**：它提供了机密客户端的安全假设（如 SAS 对机密客户端的信任等级），但实际上 secret 是公开的。

**实际风险：**

- 攻击者获取 client_secret 后，可以冒充合法客户端调用 token 端点
- 结合 2.2 的验证码绕过，可对 portal 用户发起无限制暴力破解
- `client_secret_basic` 相比 `none`（公共客户端）并未提供额外的安全保障，反而增加了运维复杂度

**修复建议（按优先级）：**

1. **短期（本次改造内）：** 将客户端改回公共客户端（`client_authentication_methods: none`），前端不携带 secret。密码模式下公共客户端的安全性并不比机密客户端差（因为 secret 本来就保不住），反而减少了攻击面和运维负担。SAS 对公共客户端的 refresh_token 签发需要额外配置（`ScaRefreshTokenGenerator` 正是为此准备的，但当前未启用——见 4.1）。

2. **中期：** 引入 BFF（Backend For Frontend）层，由 BFF 持有 client_secret，SPA 通过 httpOnly cookie 与 BFF 通信。这是 OAuth 2.1 对 SPA 的推荐架构（RFC 9700 §2.1）。

3. **长期：** 迁移到授权码+PKCE（OAuth 2.1 推荐），彻底消除密码模式的安全风险。

---

### 3.2 用户名枚举漏洞

**文件：** `sca-skeleton-auth/.../grant/base/OAuth2ResourceOwnerBaseAuthenticationProvider.java`，`mapToOAuth2AuthenticationException()` 方法（约第 528–530 行）

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

**修复建议：** 将 `UsernameNotFoundException` 和 `BadCredentialsException` 统一映射为 `"用户名或密码错误"`。同时 `ScaUserDetailsService` 中 `throw new UsernameNotFoundException("用户不存在：" + username)` 的消息也应改为不含用户名的通用消息（虽然该消息不会直接返回前端，但可能出现在日志中，且 Spring Security 的 `DaoAuthenticationProvider` 默认会将 `UsernameNotFoundException` 转换为 `BadCredentialsException`，此处影响较小）。

---

### 3.3 Token 存储在 localStorage，XSS 可窃取全部凭证

**文件：** `apps/admin/src/stores/auth.ts`、`apps/portal/src/stores/auth.ts`

```typescript
localStorage.setItem(TOKEN_STORAGE_KEY, this.token);
localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, this.refreshToken);
```

access_token 和 refresh_token 均存储在 localStorage 中。任何 XSS 漏洞（包括第三方依赖引入的）都可直接读取 `localStorage` 获取全部令牌。refresh_token 的有效期为 2 小时（默认配置），攻击窗口较大。

**风险等级说明：** 这是 SPA 令牌存储的常见权衡。httpOnly cookie 方案需要后端配合（Set-Cookie + CSRF 防护），改造成本较高。当前方案可接受，但应在安全评审中记录为已知风险，并确保 CSP 头配置严格（当前 Gateway 已配置 CSP，但 `script-src 'self' 'unsafe-inline'` 中的 `unsafe-inline` 削弱了 CSP 对 XSS 的防护）。

---

### 3.4 Gateway CORS 配置过于宽松

**文件：** `sca-skeleton-gateway-dev.yaml`

```yaml
globalcors:
  cors-configurations:
    '[/**]':
      allowed-origins: "*"
```

虽然 `allow-credentials: false`，但 `allowed-origins: "*"` 在生产环境中不应当使用。应当限制为实际的前端域名列表。

---

## 四、功能性缺陷（P2）

### 4.1 ScaRefreshTokenGenerator 是死代码

**文件：** `sca-skeleton-auth/.../token/ScaRefreshTokenGenerator.java`

该类存在完整的实现和 Javadoc（说明是为了解决 SAS 对公共客户端不签发 refresh_token 的问题），但在 `AuthorizationServerConfig.tokenGenerator()` 中实际使用的是 SAS 内置的 `OAuth2RefreshTokenGenerator`：

```java
@Bean
public OAuth2TokenGenerator<?> tokenGenerator() {
    OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
    accessTokenGenerator.setAccessTokenCustomizer(new ScaOpaqueAccessTokenClaimsCustomizer());
    OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();
    return new DelegatingOAuth2TokenGenerator(accessTokenGenerator, refreshTokenGenerator);
}
```

如果按 3.1 的建议将客户端改回公共客户端，则必须启用 `ScaRefreshTokenGenerator`（因为 SAS 内置的 `OAuth2RefreshTokenGenerator` 对公共客户端返回 null）。当前状态下该类是死代码，容易造成维护混淆。

**建议：** 要么删除该类，要么在 `tokenGenerator()` 中替换为 `new ScaRefreshTokenGenerator()`（为未来切换公共客户端做准备）。

---

### 4.2 "记住我"功能未实现

**文件：** `apps/admin/src/views/auth/LoginView.vue`、`apps/portal/src/views/auth/LoginView.vue`

两个登录页均有 `rememberMe` ref 和对应的 UI 复选框，但没有任何逻辑消费该值。用户勾选"记住我"后无任何实际效果。

**建议：** 要么实现功能（如延长 refresh_token 有效期、将用户名存入 localStorage），要么移除 UI 元素避免误导用户。

---

### 4.3 "忘记密码"和"其他登录方式"是死链接

**文件：** 同上两个 LoginView.vue

```html
<a href="#" class="md3-link md3-body-medium" tabindex="-1" @click.prevent>忘记密码？</a>
```

`@click.prevent` 阻止了默认行为但没有执行任何操作。"其他登录方式"（手机、扫码、指纹）也是纯 UI 占位。

**建议：** 如果短期内不会实现，应添加 `disabled` 状态或 `title="即将开放"` 提示，避免用户反复点击无响应。

---

### 4.4 SQL 安装脚本 token_settings 不完整

**文件：** `deploy/sql/install/sca_platform.sql`

```sql
'{"settings.token.reuse-refresh-tokens":false}'
```

缺少 `settings.token.access-token-time-to-live`、`settings.token.refresh-token-time-to-live`、`settings.token.access-token-format` 等关键配置。虽然 `OAuth2RegisteredClientInitializer` 会在应用启动时修复，但 SQL 脚本作为独立安装入口应当是自洽的——如果只执行 SQL 不启动应用，客户端配置是不完整的。

---

## 五、代码质量与规范性问题（P3）

### 5.1 Javadoc 引用已删除的类

| 文件 | 问题 |
|------|------|
| `LoginChannelContext.java` | Javadoc 写"由 LoginChannelFilter 写入"，但 LoginChannelFilter 已删除，实际由 `OAuth2ResourceOwnerBaseAuthenticationProvider.authenticate()` 写入 |
| `LoginLogPublisher.java` `calculateCostMs()` | Javadoc 写"从请求属性取出 LoginChannelFilter 记录的开始时间"，实际由 `OAuth2ResourceOwnerBaseAuthenticationProvider.recordLoginStartTime()` 写入 |
| `AuthLoginProperties.java` | Javadoc 写"通过 Thymeleaf 注入到 admin / portal 登录页模板"，但 Thymeleaf 模板已删除 |

---

### 5.2 手工拼接 JSON 存在注入风险

**文件：** `AuthorizationServerConfig.java`，`oauth2TokenEndpointAuthenticationEntryPoint()` 方法

```java
String message = rawMessage
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
    .replace("\r", "\\r")
    .replace("\t", "\\t");
String json = String.format(
    "{\"error\":\"unauthorized\",\"error_description\":\"%s\",\"timestamp\":%d}",
    message, Instant.now().toEpochMilli());
```

手工转义不完整：未处理 Unicode 控制字符（如 `\u0000`–`\u001F` 中除 `\n\r\t` 外的字符）。应使用 Jackson `ObjectMapper` 或 `JsonMapper` 序列化。

同样的问题存在于 `CaptchaVerificationFilter.writeCaptchaError()` 中。

---

### 5.3 `@SuppressWarnings("deprecation")` 无实际作用

**文件：** `AuthorizationServerConfig.java`，`passwordAuthenticationProvider()` 方法

该方法内部未使用任何 `@Deprecated` API，注解是多余的。

---

### 5.4 Admin 与 Portal LoginView.vue 大量重复代码

两个登录页组件约 80% 的代码相同（轮播图逻辑、Toast、键盘交互、表单验证、模板结构），仅验证码部分不同。应提取为：

- 共享 composable：`useCarousel()`、`useToast()`、`useKeyboardNavigation()`
- 共享组件：`LoginFormBase.vue`（通过 slot 插入验证码区域）

当前状态下，任何 UI 修改都需要同步改两个文件，维护成本高且容易遗漏。

---

### 5.5 前端 scope 硬编码

**文件：** `packages/shared/src/oauth/password-grant.ts`，`readOAuthConfigFromEnv()`

```typescript
scope: "profile all"
```

scope 未从环境变量读取，修改 scope 需要改代码重新构建。建议增加 `VITE_OAUTH_SCOPE` 环境变量，默认值 `"profile all"`。

---

### 5.6 Gateway 限流阈值偏宽松

**文件：** `sca-skeleton-gateway-dev.yaml`

```yaml
redis-rate-limiter.replenishRate: 15
redis-rate-limiter.burstCapacity: 30
```

密码模式下 token 端点直接接受用户名密码，15 req/s 的持续速率和 30 的突发容量对暴力破解的防护偏弱。虽有账号锁定（5 次失败锁定 30 分钟），但限流是第一道防线，建议收紧到 5/10 或按 IP+用户名组合限流。

---

## 六、架构设计评价

### 6.1 优点

1. **Converter → Provider → Token 三层抽象清晰**：Base 类提取了公共逻辑，子类只需关注 grant_type 特有行为。未来扩展短信验证码模式（`grant_type=sms`）只需新增一组 Converter/Provider/Token，无需修改 Base 层。

2. **LoginChannelContext + RoutingUserDetailsService 的渠道路由设计**：通过 ThreadLocal 在 Provider 内部完成渠道设置与清理，避免了额外 Filter 的引入。finally 块确保 ThreadLocal 清理，防止线程池复用串扰。

3. **OAuth2RegisteredClientInitializer 的渐进式迁移策略**：`repairCorruptedClientIfNeeded` → `initConfidentialClientIfAbsent` → `migratePublicClientIfNeeded` → `migrateToOpaqueAccessTokenIfNeeded` 四步走，兼容了从旧版本升级的场景。

4. **LoginLogPublisher 与 LoginAttemptEventListener 职责分离**：前者负责审计日志，后者负责安全策略（锁定），各司其职。

5. **前端 axios-oauth.ts 的 refresh 去重**：`refreshPromise` 单例模式确保并发 401 时只触发一次 refresh，避免 refresh_token 被多次消费（`reuseRefreshTokens=false` 下多次消费会导致后续 refresh 失败）。

### 6.2 设计层面的担忧

1. **密码模式 + 机密客户端的组合在 SPA 场景下缺乏安全增益**（见 3.1 详细分析）。如果坚持使用密码模式，公共客户端是更诚实的选择。

2. **LoginChannelContext 的 ThreadLocal 传递是脆弱设计**：当前 `LoginAttemptEventListener` 和 `LoginLogPublisher` 是同步 `@EventListener`，ThreadLocal 可用。但如果未来有人加上 `@Async` 或使用 `ApplicationEventMulticaster` 的异步模式，ThreadLocal 立即失效。建议在事件对象中显式携带 channel 信息，而非依赖 ThreadLocal 隐式传递。

3. **OAuth2RegisteredClientInitializer 中 `repairCorruptedClientIfNeeded` 直接 DELETE 数据库记录**：生产环境中自动删除数据的行为过于激进。建议改为：检测到不兼容记录时记录 ERROR 日志并跳过初始化（fail-fast），由运维手动处理。

---

## 七、与 pig 项目的对比

| 维度 | pig | sca-skeleton（本次改造后） | 评价 |
|------|-----|--------------------------|------|
| 授权模式扩展 | 自定义 `OAuth2ResourceOwnerBaseAuthenticationToken` + Converter + Provider | 同架构，Base 泛型抽象 | 持平，sca 的 Javadoc 更完整 |
| 渠道路由 | 额外 `LoginChannelFilter` + 请求参数 | Provider 内部 ThreadLocal | sca 更内聚，但 ThreadLocal 传递更脆弱 |
| 依赖注入 | hutool `SpringUtil` 静态获取 | 构造器注入 | sca 更规范 |
| 异常处理 | 自定义错误码扩展 | 标准 OAuth2 错误码 | sca 更符合规范 |
| 验证码 | 独立 Filter + 请求参数 client_id | 同 | 两者都有 client_id 获取方式的问题 |
| 登录日志 | 与认证逻辑耦合 | EventListener 解耦 | sca 更优 |
| 令牌格式 | JWT（自包含） | 不透明令牌（REFERENCE）+ Redis 自省 | 各有取舍，sca 支持即时吊销 |
| 代码注释 | 较少 | 详尽的 Javadoc + 行内注释 | sca 明显更优 |

**结论：** 在编码规范性、注释完整度、架构解耦程度上，本次改造确实优于 pig。但在安全性设计上（client_secret 前端暴露、用户名枚举），存在与 pig 相同甚至更突出的问题。"比 pig 强"的目标在代码质量维度达成，在安全维度未达成。

---

## 八、E2E 测试覆盖评价

当前仅有一个 `session-timeout-redirect.spec.ts` 测试用例，覆盖严重不足。建议补充：

- 登录成功 → 跳转首页 → 侧栏菜单正确渲染
- 登录失败（错误密码）→ Toast 提示 → 不跳转
- Portal 登录 → 验证码加载 → 输入验证码 → 登录成功
- Token 过期 → 静默 refresh → 请求正常完成
- Refresh token 过期 → 重定向到登录页
- 退出登录 → 令牌吊销 → 访问受保护路由被拦截

---

## 九、问题汇总

| # | 严重度 | 类别 | 摘要 | 文件 |
|---|--------|------|------|------|
| 2.1 | P0 | 功能 | Portal LoginChannel 判定失效，portal 用户无法登录 | OAuth2ResourceOwnerPasswordAuthenticationProvider.java |
| 2.2 | P0 | 功能 | CaptchaVerificationFilter 无法识别 portal，验证码校验被跳过 | CaptchaVerificationFilter.java |
| 3.1 | P1 | 安全 | client_secret 硬编码在前端 .env，构建后公开可见 | .env.development / .env.production (admin+portal) |
| 3.2 | P1 | 安全 | 用户名枚举：不同错误消息区分"用户不存在"与"密码错误" | OAuth2ResourceOwnerBaseAuthenticationProvider.java |
| 3.3 | P1 | 安全 | Token 存 localStorage，XSS 可窃取全部凭证 | stores/auth.ts (admin+portal) |
| 3.4 | P1 | 安全 | Gateway CORS allowed-origins: "*" | sca-skeleton-gateway-dev.yaml |
| 4.1 | P2 | 功能 | ScaRefreshTokenGenerator 死代码 | ScaRefreshTokenGenerator.java |
| 4.2 | P2 | 功能 | "记住我"未实现 | LoginView.vue (admin+portal) |
| 4.3 | P2 | 功能 | "忘记密码"/"其他登录方式"死链接 | LoginView.vue (admin+portal) |
| 4.4 | P2 | 功能 | SQL token_settings 不完整 | sca_platform.sql |
| 5.1 | P3 | 规范 | Javadoc 引用已删除的 LoginChannelFilter / Thymeleaf | LoginChannelContext / LoginLogPublisher / AuthLoginProperties |
| 5.2 | P3 | 规范 | 手工拼接 JSON，转义不完整 | AuthorizationServerConfig / CaptchaVerificationFilter |
| 5.3 | P3 | 规范 | 多余的 @SuppressWarnings("deprecation") | AuthorizationServerConfig.java |
| 5.4 | P3 | 规范 | Admin/Portal LoginView 80% 代码重复 | LoginView.vue (admin+portal) |
| 5.5 | P3 | 规范 | scope 硬编码 | password-grant.ts |
| 5.6 | P3 | 规范 | Gateway 限流阈值偏宽松 | sca-skeleton-gateway-dev.yaml |

---

## 十、修复优先级建议

1. **立即修复（阻断性）：** 2.1 + 2.2 — 不修复则 portal 渠道完全不可用
2. **本轮修复（安全）：** 3.1（至少改为公共客户端或移除前端 secret）、3.2（统一错误消息）
3. **下轮迭代：** 4.1–4.4、5.1–5.6
4. **长期规划：** BFF 架构（解决 3.3）、OAuth 2.1 授权码+PKCE 回归

---

*评审完毕。以上所有问题均基于代码静态分析，未执行运行时验证。建议修复 P0 问题后补充 portal 渠道的集成测试。*
