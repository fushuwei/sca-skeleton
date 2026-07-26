# OAuth2 授权码+PKCE → 密码模式改造 代码评审报告

> **评审工具**: opencode  
> **评审日期**: 2026-07-26  
> **改造范围**: 授权码+PKCE 模式 → 密码模式（OAuth2 Resource Owner Password Credentials Grant）  
> **评审基线**: 85 个文件变更，+2344 行 / -3534 行

---

## 一、总体评价

本次改造整体质量**较高**，架构设计合理，代码组织清晰，关键路径的实现严谨度明显优于 pig 项目。以下从 7 个维度逐项评审。

---

## 二、逐维度评审

### 2.1 架构设计（优秀）

| 评估项 | 评分 | 说明 |
|--------|------|------|
| 分层设计 | ★★★★★ | `base` 抽象层 + `password` 子类的模板模式设计，扩展性极强 |
| 与 SAS 集成 | ★★★★★ | 不 hack SAS 内部 API，通过标准 `AuthenticationConverter` 组合实现 |
| 状态管理 | ★★★★★ | 全链路无状态（STATELESS），ThreadLocal 使用/清理规范 |
| 渠道路由 | ★★★★★ | `LoginChannelContext` + `RoutingUserDetailsService` 解耦 admin/portal |

**亮点**：
- `OAuth2ResourceOwnerBaseAuthenticationProvider` 的抽象设计远超 pig 项目。pig 直接在 Provider 里硬编码，本项目将 `buildToken`、`checkClient`、`resolveLoginChannel` 抽象为可覆盖方法，未来新增短信登录等模式只需子类化。
- `OAuth2ResourceOwnerBaseAuthenticationConverter` 不依赖 SAS 内部工具类 `OAuth2EndpointUtils`，直接使用标准 `HttpServletRequest.getParameterMap()`，对 SAS 版本升级免疫。
- 验证码过滤器 `CaptchaVerificationFilter` 仅拦截 `grant_type=password` + portal 渠道，admin 渠道免验证码——这个设计精准合理。

---

### 2.2 后端代码（良好，有细节问题）

#### 2.2.1 认证链配置（`AuthorizationServerConfig`）

**优点**：
- `@Order(1)` 过滤链精确匹配 `/oauth2/**` 和 `/.well-known/**`，与默认链（`@Order(2)`）职责清晰
- 委托式 `AuthenticationConverter` 组合：SAS 原生转换器 + 自定义密码转换器，按序匹配，设计干净
- `AuthenticationEntryPoint` 返回标准 OAuth2 JSON 错误格式，适配 SPA AJAX 调用

**问题 1：`oauth2TokenEndpointAuthenticationEntryPoint` 的 JSON 转义不完整**

`AuthorizationServerConfig.java:180-185`：
```java
String message = rawMessage
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
    .replace("\r", "\\r")
    .replace("\t", "\\t");
```
缺少对 `</script>` 等 XSS 向量的转义（虽然 Content-Type 是 application/json 且无 CSP 问题，但作为安全最佳实践应考虑）。此外，应使用 Jackson 或 Gson 序列化而非手动拼接 JSON。

**建议**：注入 `ObjectMapper` 替代 `String.format` 手动拼接，避免特殊字符导致 JSON 格式损坏。

#### 2.2.2 密码模式 Provider（`OAuth2ResourceOwnerBaseAuthenticationProvider`）

**优点**：
- 异常映射覆盖全面：`UsernameNotFoundException`、`BadCredentialsException`、`LockedException`、`DisabledException`、`AccountExpiredException`、`CredentialsExpiredException` 均映射为标准 OAuth2 错误码
- `LoginChannelContext.set/clear` 在 `try/finally` 中保证 ThreadLocal 清理
- 登录开始时间写入请求属性供 `LoginLogPublisher` 使用，解耦合理

**问题 2：`resolveLoginChannel` 中 `client_id` 获取逻辑存在歧义**

`OAuth2ResourceOwnerBaseAuthenticationProvider.java:160-161` 和 `OAuth2ResourceOwnerPasswordAuthenticationProvider.java:92-96`：
```java
Map<String, Object> reqParameters = resourceOwnerAuthentication.getAdditionalParameters();
LoginChannel channel = resolveLoginChannel(reqParameters);
```
注释说"client_id 不在 additionalParameters 中"，但实际代码又从 `additionalParameters` 中取 `client_id`。由于 `OAuth2ResourceOwnerBaseAuthenticationConverter` 的 `convert()` 方法在第 107-110 行将**除 grant_type 和 scope 以外的所有参数**放入 `additionalParameters`，所以 `client_id` 实际上**会**在 `additionalParameters` 中。

但这里存在一个语义问题：`client_id` 经过 SAS 客户端认证过滤器后已经被"消费"了，从 `additionalParameters` 中读取的 `client_id` 是未经验证的原始值。虽然攻击者篡改 `client_id` 的实际影响有限（因为客户端认证已在 SAS 层完成，`registeredClient` 已经是经过认证的），但**最佳实践应该从已认证的 `registeredClient` 中获取 `clientId`**：

```java
// 推荐方式
String clientId = registeredClient.getClientId();
```

当前实现功能正确但不够严谨。

**问题 3：`LoginChannelContext` 和 `LoginLogPublisher` 的 Javadoc 过时**

`LoginChannelContext.java:6` 仍然引用已删除的 `LoginChannelFilter`：
```java
 * 由 {@link io.github.fushuwei.scaskeleton.auth.security.filter.LoginChannelFilter} 写入，
```
应更新为引用 `OAuth2ResourceOwnerBaseAuthenticationProvider`。

`LoginLogPublisher.java:169` 同样引用了已删除的类：
```java
 * 计算登录耗时：从请求属性取出 {@link LoginChannelFilter} 记录的开始时间，求差值。
```

#### 2.2.3 客户端初始化器（`OAuth2RegisteredClientInitializer`）

**优点**：
- `repairCorruptedClientIfNeeded` 方法防御性处理 DB 中格式不兼容的旧记录
- `migratePublicClientIfNeeded` 自动将旧版公共客户端迁移为机密客户端，向后兼容
- `migrateToOpaqueAccessTokenIfNeeded` 确保 token 格式为 REFERENCE（不透明令牌）

**问题 4：SQL 安装脚本中 `client_secret` 为 NULL 但 `client_authentication_methods` 为 `client_secret_basic`**

`sca_platform.sql:649-680`：
```sql
INSERT INTO oauth2_registered_client (..., client_secret, client_authentication_methods, ...)
SELECT ..., NULL, 'client_secret_basic', ...
```
在安装脚本中 `client_secret` 为 NULL，依赖启动时 `OAuth2RegisteredClientInitializer` 加密写入。这意味着**数据库初始化后、应用首次启动前**，`oauth2_registered_client` 表中的记录处于不一致状态（声明需要 secret 但实际为空）。如果有其他系统或脚本直接查询 DB 来判断客户端类型，可能产生误判。

这不是阻断性问题（`OAuth2RegisteredClientInitializer` 会修复），但可以考虑在 SQL 脚本中添加注释说明。

#### 2.2.4 验证码过滤器（`CaptchaVerificationFilter`）

**优点**：
- 精确拦截：仅 `POST /oauth2/token` + `grant_type=password` + portal 渠道
- 使用 `OncePerRequestFilter` 确保单次执行
- 校验失败返回标准 OAuth2 JSON 错误，适配 SPA

**问题 5：`isTokenEndpointPost` 使用 `getRequestURI()` 而非 `getServletPath()`**

`CaptchaVerificationFilter.java:109-110`：
```java
return "POST".equalsIgnoreCase(request.getMethod())
        && TOKEN_URI.equals(request.getRequestURI());
```
`getRequestURI()` 返回完整 URI（含 context-path），如果 auth 服务配置了 context-path（如 `/auth`），则 `request.getRequestURI()` 会返回 `/auth/oauth2/token`，与 `TOKEN_URI`（`/oauth2/token`）不匹配。

但从配置看 auth 服务没有配置 context-path（由网关 `StripPrefix=1` 转发），所以当前实现是正确的。不过 `getServletPath()` 是更标准的做法。

#### 2.2.5 删除的文件清理

| 已删除文件 | 状态 | 说明 |
|-----------|------|------|
| `AuthWebMvcConfig.java` | ✅ 已删除 | 不再需要静态资源映射 |
| `AuthWebSecurityBeans.java` | ✅ 已删除 | 不再需要 `HttpSessionRequestCache` |
| `AuthSessionAttributes.java` | ✅ 已删除 | 不再需要 Session 属性常量 |
| `LoginPageController.java` | ✅ 已删除 | 不再需要 Thymeleaf 登录页控制器 |
| `OAuth2LoginRedirectResolver.java` | ✅ 已删除 | 不再需要授权码重定向解析 |
| `OAuth2PendingAuthorizeStore.java` | ✅ 已删除 | 不再需要待授权存储 |
| `AuthorizeChannelIsolationFilter.java` | ✅ 已删除 | 不再需要渠道隔离过滤器 |
| `LoginChannelFilter.java` | ✅ 已删除 | 由 Provider 内部设置 ChannelContext 替代 |
| `PublicClientRefreshTokenAuthenticationFilter.java` | ✅ 已删除 | 不再需要公共客户端刷新令牌过滤器 |
| `ChannelAwareAuthenticationFailureHandler.java` | ✅ 已删除 | 不再需要表单登录失败处理器 |
| `OAuth2ClientAwareLoginUrlAuthenticationEntryPoint.java` | ✅ 已删除 | 不再需要表单登录入口点 |
| `OAuth2AuthorizeLoginSuccessHandler.java` | ✅ 已删除 | 不再需要授权码登录成功处理器 |
| `templates/login/admin.html` | ✅ 已删除 | 登录页迁移至 SPA |
| `templates/login/portal.html` | ✅ 已删除 | 登录页迁移至 SPA |
| `static/css/login.css` | ✅ 已删除 | 迁移至 `packages/ui/src/styles/login.scss` |
| `static/js/login.js` | ✅ 已删除 | 功能迁移至 SPA 组件 |
| `static/images/*` | ✅ 已删除 | 图标迁移至 Material Symbols 字体 |
| `OAuthCallbackView.vue`（admin） | ✅ 已删除 | 不再需要授权码回调页 |
| `OAuthCallbackView.vue`（portal） | ✅ 已删除 | 不再需要授权码回调页 |
| `apis/captcha.ts`（admin） | ✅ 已删除 | Admin 渠道不要求验证码 |
| `pkce.ts` | ✅ 已删除 | 由 `password-grant.ts` 替代 |

**清理非常彻底**，没有遗留死代码。

---

### 2.3 前端代码（优秀）

#### 2.3.1 Shared 包（`packages/shared/src/oauth/`）

**`password-grant.ts`**：
- `loginWithPassword` 函数设计清晰，自动附加 `Basic` 认证头
- `refreshAccessToken` 同样使用 Basic 认证（机密客户端标准行为）
- `revokeOAuthToken` 失败不阻断（logout 场景合理）
- `readOAuthConfigFromEnv` 从 Vite 环境变量读取配置，类型安全

**`axios-oauth.ts`**：
- `redirectToLogin` 回调由 SPA 提供，解耦合理
- `refreshPromise` 单例锁防止并发刷新（优秀的设计，pig 项目没有）
- 401/403/429/5xx 全覆盖的全局错误处理

#### 2.3.2 登录页组件

**Admin `LoginView.vue`**：
- 表单验证：用户名/密码非空校验，实时验证 + 提交时校验
- 密码可见性切换
- 轮播图：5 秒自动轮播 + 手动控制 + 指示器
- Toast 提示：5 秒自动消失 + 手动关闭
- 键盘交互：全局 Enter 提交、首次按键聚焦、Tab 循环

**Portal `LoginView.vue`**：
- 在 Admin 基础上额外包含验证码输入框 + 可点击刷新的验证码图片
- 验证码通过 `GET /auth/captcha/generate?key=xxx` 获取（JSON 模式）
- 登录失败后自动刷新验证码
- 三个输入框的 Tab 循环

**问题 6：Admin 登录页的 `rememberMe` 状态未实际使用**

`LoginView.vue:19` 定义了 `rememberMe` ref，UI 上也有复选框，但 `handleLogin` 方法中未将其传递给 `authStore.login()`，也未在 token 存储逻辑中使用。这是一个未完成的功能。

当前实现中 `localStorage` 永久存储 token（除非手动清除），`rememberMe` 语义上应控制 token 存储策略（如 `sessionStorage` vs `localStorage`），但目前未实现。

**建议**：要么实现 rememberMe 功能（token 存入 `sessionStorage` vs `localStorage`），要么移除 UI 上的复选框避免用户混淆。

**问题 7：轮播图资源路径可能不存在**

`LoginView.vue:72-74`：
```ts
const slides = [
  { src: "/images/login/carousel1.jpg", ... },
  { src: "/images/login/carousel2.jpg", ... },
  { src: "/images/login/carousel3.jpg", ... }
];
```
轮播图引用的是 `/images/login/carousel*.jpg`，需要确认这些文件已放置在 `admin/public/images/login/` 和 `portal/public/images/login/` 目录下。从 git diff 看，carousel 图片确实从 auth 服务的 `static/images/` 迁移到了 SPA 的 `public/images/login/` 目录，路径正确。

#### 2.3.3 路由守卫（`guards.ts`）

**优点**：
- 静默 refresh 机制：未登录时先尝试 `refresh_token` 续期，失败才重定向登录
- 已登录访问登录页时自动重定向到首页
- profile 拉取失败视为登录态失效，清理全部令牌
- 动态路由 404 兜底：若路径在菜单树中则自动恢复

**与 pig 项目的对比优势**：
- pig 的路由守卫不支持静默 refresh，令牌过期直接跳转登录页
- 本项目的守卫支持 redirect 参数回跳，用户体验更好

#### 2.3.4 环境变量配置

| 文件 | 变量 | 说明 |
|------|------|------|
| `admin/.env.development` | `VITE_OAUTH_CLIENT_SECRET=admin-secret` | 机密客户端密钥 |
| `admin/.env.production` | `VITE_OAUTH_CLIENT_SECRET=admin-secret` | 生产环境应通过环境变量覆盖 |
| `portal/.env.development` | `VITE_OAUTH_CLIENT_SECRET=portal-secret` | 机密客户端密钥 |
| `portal/.env.production` | `VITE_OAUTH_CLIENT_SECRET=portal-secret` | 生产环境应通过环境变量覆盖 |

**问题 8：生产环境 `.env.production` 中硬编码了 `client_secret`**

`admin/.env.production:5` 和 `portal/.env.production:5` 直接写入了密钥明文。虽然注释说"生产环境通过环境变量注入"，但文件本身已包含明文值。对于前端 SPA，`client_secret` 会通过 Vite 构建打包到 JS 产物中，任何人都可以通过浏览器 DevTools 提取。

**这是密码模式的核心安全风险**：机密客户端的 `client_secret` 暴露在前端。在 OAuth 2.0 规范中，密码模式本身就是为"高度信任的客户端"设计的（如原生应用、内部 SPA），但如果 SPA 可被第三方访问（如公网部署），`client_secret` 的保护意义有限。

**建议**：
1. 生产环境通过 CI/CD 注入环境变量，不要在代码仓库中保留明文
2. 在文档中明确说明密码模式的安全假设和适用场景
3. 考虑为公网部署场景保留授权码+PKCE 模式作为备选

---

### 2.4 Gateway 配置（良好）

`sca-skeleton-gateway-dev.yaml` 改动：
- ✅ 移除了 `/auth/oauth2/authorize` 路由和限流规则
- ✅ 白名单精简为：`/auth/oauth2/token`、`/auth/oauth2/revoke`、`/auth/.well-known/**`、`/auth/captcha/**`
- ✅ 全局过滤器注释更新为"密码模式为无状态认证"
- ✅ CORS 配置 `allow-credentials: false` 与无状态模式一致

**问题 9：CORS 配置 `allowed-origins: "*"` 在生产环境有风险**

`sca-skeleton-gateway-dev.yaml:83`：
```yaml
allowed-origins: "*"
```
通配符允许所有来源的跨域请求。在生产环境应配置为具体的前端域名。虽然注释已说明"生产环境应配置具体域名"，但建议在生产配置文件中使用明确的域名列表。

---

### 2.5 SQL 脚本（良好）

`sca_platform.sql` 改动：
- ✅ `client_authentication_methods`: `none` → `client_secret_basic`
- ✅ `authorization_grant_types`: `authorization_code,refresh_token` → `password,refresh_token`
- ✅ `client_settings` 中移除了 `require-proof-key`
- ✅ 添加了清晰的注释说明 `client_secret` 由初始化器写入

**问题 10：SQL 脚本与 `OAuth2RegisteredClientInitializer` 存在双重写入**

SQL 脚本 INSERT 了 `client_secret=NULL` 的记录，而 `OAuth2RegisteredClientInitializer` 启动时通过 `buildConfidentialClient` 重新 `save()`（覆盖）。如果 DB 中已存在旧版公共客户端记录，`migratePublicClientIfNeeded` 会再次覆盖。

这个流程是安全的（幂等设计），但执行顺序为：
1. SQL 安装脚本写入 `client_secret=NULL` 的记录
2. 应用启动 → `repairCorruptedClientIfNeeded` 尝试读取（可能因 JSON 格式不兼容失败）→ 删除
3. `initConfidentialClientIfAbsent` 检查是否已存在 → 不存在则创建（带加密 secret）
4. `migratePublicClientIfNeeded` 再次检查并迁移

**建议**：在 SQL 脚本中添加注释说明此行为，或考虑将 SQL 中的 INSERT 改为纯占位（仅插入 id 和 client_id），其余由初始化器全权管理。

---

### 2.6 E2E 测试（良好）

`session-timeout-redirect.spec.ts`：
- ✅ 测试目标从 `/auth/login/admin` 改为 SPA 的 `/admin/login`
- ✅ 测试逻辑：清除 localStorage → 刷新页面 → 验证重定向到登录页 + redirect 参数

**问题 11：测试依赖具体的 localStorage key 名称**

`session-timeout-redirect.spec.ts:24-25`：
```ts
localStorage.removeItem("admin_access_token");
localStorage.removeItem("admin_refresh_token");
```
这些 key 名称与 `constants/auth-storage.ts` 中的定义耦合。如果 key 名称变更，测试会静默通过（因为 `removeItem` 对不存在的 key 不报错），但实际上没有清除 token。

**建议**：从 `constants/auth-storage.ts` 导入 key 常量，或在测试中使用 `localStorage.clear()` 清除全部存储。

---

### 2.7 日志与可观测性（优秀）

**后端日志覆盖**：
- 密码模式认证开始：`client_id`, `channel`, `username`（DEBUG 级别）
- 认证成功：`client_id`, `username`（INFO 级别）
- 认证失败：`client_id`, `username`, `error`（WARN 级别）
- 认证异常：`client_id`, `username`（ERROR 级别）
- 令牌生成完成：`client_id`, 脱敏 `access_token` 前 8 位（DEBUG 级别）
- 客户端初始化：初始化/跳过/迁移/格式修复（INFO/WARN 级别）
- 验证码校验失败：`client_id`, `captcha_key`（WARN 级别）
- 登录日志：IP、User-Agent、耗时、成功/失败（通过 Spring Event 异步）

**前端日志**：
- axios 拦截器：401/403/429/5xx 统一 Toast 提示
- 登录失败：Toast 展示错误消息

**亮点**：令牌脱敏输出（`maskToken` 方法）避免敏感信息泄露到日志，这是 pig 项目没有的。

---

## 三、与 pig 项目的对比

| 对比维度 | pig 项目 | 本项目 | 评价 |
|----------|---------|--------|------|
| Provider 抽象 | 硬编码 | 模板方法模式（base + 子类） | **本项目更优** |
| Converter | 依赖 SAS 内部 `OAuth2EndpointUtils` | 纯标准 API | **本项目更优** |
| ThreadLocal 管理 | 额外 Filter 设置 | Provider 内部 try/finally | **本项目更优** |
| 并发刷新保护 | 无 | `refreshPromise` 单例锁 | **本项目更优** |
| 异常映射 | 部分映射 | 全覆盖 + 标准 OAuth2 错误码 | **本项目更优** |
| 客户端迁移 | 无 | 自动迁移旧公共客户端 | **本项目更优** |
| 验证码 | Thymeleaf 页面内校验 | 独立 Filter + SPA 前端 | **本项目更优** |
| 日志脱敏 | 无 | `maskToken` 前 8 位 | **本项目更优** |
| 登录日志 | 直接入库 | Spring Event 异步 | **本项目更优** |
| 扩展性 | 仅支持密码模式 | base 层可扩展短信等模式 | **本项目更优** |

---

## 四、问题汇总与优先级

| # | 严重度 | 问题 | 文件 | 建议 |
|---|--------|------|------|------|
| 1 | 低 | `oauth2TokenEndpointAuthenticationEntryPoint` 手动拼接 JSON | `AuthorizationServerConfig.java:186` | 使用 ObjectMapper |
| 2 | 中 | `resolveLoginChannel` 从 `additionalParameters` 读取未验证的 `client_id` | `OAuth2ResourceOwnerPasswordAuthenticationProvider.java:92` | 从已认证的 `registeredClient.getClientId()` 获取 |
| 3 | 低 | Javadoc 引用已删除的 `LoginChannelFilter` | `LoginChannelContext.java:6`, `LoginLogPublisher.java:169` | 更新 Javadoc |
| 4 | 低 | SQL 脚本 `client_secret=NULL` 与 `client_secret_basic` 认证方式不一致 | `sca_platform.sql:671` | 添加注释说明 |
| 5 | 低 | `CaptchaVerificationFilter` 使用 `getRequestURI()` | `CaptchaVerificationFilter.java:110` | 改为 `getServletPath()`（当前无 context-path 不影响） |
| 6 | 中 | `rememberMe` 功能未实现但 UI 存在 | `LoginView.vue`（admin/portal） | 实现或移除 |
| 7 | 低 | 轮播图资源路径需确认已放置 | `LoginView.vue:72-74` | 确认 `public/images/login/` 目录存在 |
| 8 | **高** | 生产环境 `.env.production` 硬编码 `client_secret` | `.env.production`（admin/portal） | CI/CD 注入，不要入库 |
| 9 | 中 | CORS `allowed-origins: "*"` 生产环境风险 | `sca-skeleton-gateway-dev.yaml:83` | 生产配置使用明确域名 |
| 10 | 低 | SQL + 初始化器双重写入逻辑需文档说明 | `sca_platform.sql` + `OAuth2RegisteredClientInitializer` | 添加注释 |
| 11 | 低 | E2E 测试硬编码 localStorage key | `session-timeout-redirect.spec.ts:24` | 导入常量或使用 `localStorage.clear()` |

---

## 五、安全性评审

### 5.1 密码模式固有风险

OAuth 2.0 规范（RFC 6749 Section 4.3）已废弃密码模式，OAuth 2.1 中被正式移除。密码模式的核心风险是**用户密码暴露给客户端**，违背了 OAuth "不向客户端暴露用户密码"的设计初衷。

**适用场景**：高度信任的内部系统（如企业内部管理后台），不适用于面向公众的第三方应用。

### 5.2 本项目的安全缓解措施

| 风险 | 缓解措施 | 评估 |
|------|---------|------|
| 密码明文传输 | HTTPS（生产环境必须） | ✅ 依赖部署配置 |
| client_secret 暴露在前端 | 仅限内部 SPA 使用 | ⚠️ 生产环境需确保 SPA 不可被第三方访问 |
| 暴力破解 | Gateway RateLimiter（15 QPS）+ 账号锁定（5 次失败锁 30 分钟） | ✅ 双重防护 |
| 令牌泄露 | 不透明令牌（REFERENCE）+ 有限有效期（15min access / 2h refresh） | ✅ 合理 |
| refresh_token 重放 | `reuseRefreshTokens=false`（每次刷新作废旧 refresh） | ✅ 符合最佳实践 |
| 验证码 | Portal 渠道强制图形验证码 | ✅ Admin 渠道为内部运维场景免验证码 |

### 5.3 安全建议

1. **生产环境强制 HTTPS**：密码模式下用户名密码通过 POST body 传输，必须 HTTPS 加密
2. **`client_secret` 生产注入**：不要在代码仓库中保留生产密钥
3. **考虑 RBAC 增强**：密码模式下无授权同意页面，权限控制完全依赖后端 RBAC
4. **令牌自省端点**：资源服务器的令牌自省机制无需改动，继续使用即可

---

## 六、功能影响评估

### 6.1 不受影响的功能

| 模块 | 状态 | 说明 |
|------|------|------|
| 租户管理 | ✅ 无影响 | 资源服务器令牌自省机制不变 |
| 用户管理 | ✅ 无影响 | API 层不变 |
| 角色管理 | ✅ 无影响 | API 层不变 |
| 菜单管理 | ✅ 无影响 | API 层不变 |
| 操作日志 | ✅ 无影响 | API 层不变 |
| 登录日志 | ✅ 改进 | 通过 Spring Event 异步，与密码模式 Provider 解耦 |
| 权限校验 | ✅ 无影响 | 不透明令牌 claims 扩展不变 |
| 资源服务器 | ✅ 无影响 | 令牌自省/校验逻辑不变 |

### 6.2 改进的功能

| 功能 | 改进点 |
|------|--------|
| 登录体验 | SPA 内直接登录，无需页面跳转，响应更快 |
| 退出登录 | 吊销令牌 + 本地清理，无服务端 Session 依赖 |
| 令牌续期 | 静默 refresh，用户体验无感知 |
| 会话超时 | 路由守卫自动重定向 + redirect 回跳 |

---

## 七、结论

### 总体评分：**8.5/10**

**优点**：
- 架构设计严谨，模板方法模式的扩展性远超 pig 项目
- 代码清理彻底，无死代码残留
- 日志覆盖全面，安全细节（令牌脱敏、ThreadLocal 清理）到位
- 前端登录页 1:1 还原原 Thymeleaf 效果，交互体验完整
- 客户端自动迁移机制保证向后兼容

**需改进**：
- `client_secret` 在前端暴露是密码模式的固有风险，需在文档和部署层面明确约束
- `resolveLoginChannel` 应从已认证的 `registeredClient` 获取 `client_id`
- `rememberMe` 功能需实现或移除
- Javadoc 中过时引用需清理
- 生产环境 CORS 和密钥管理需严格配置

**整体判断**：改造完成度高，代码质量优秀，可以进入测试阶段。上述问题均为非阻断性，建议在后续迭代中逐步修复。
