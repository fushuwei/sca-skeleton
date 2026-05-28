# 登录后浏览器跳转到 Auth 服务直连地址 `http://192.168.1.106:9001/login/admin`

## 现象

1. 访问 `http://localhost:5173` → 浏览器跳转到 `http://localhost:9999/auth/login/admin`（正确）
2. 输入 admin/admin → **浏览器跳转到 `http://192.168.1.106:9001/login/admin`，仍然显示登录页**（错误，Auth 服务直连地址）
3. 手动访问 `localhost:5173` → 浏览器重定向到 `http://localhost:5173/api-dev/auth/oauth2/authorize?client_id=...` → SPA 触发后端接口 → **500 错误**

## 根因分析

### 直接原因

**网关未透传 `X-Forwarded-Host`，导致 Auth 服务 `ForwardedHeaderTransformer` 使用自身地址重建 URL。**

Spring Cloud Gateway 默认不会设置 `X-Forwarded-Host` 头。Auth 服务配置了 `server.forward-headers-strategy: framework`，当 `ForwardedHeaderTransformer` 处理请求时：

- 读到 `X-Forwarded-Prefix: /auth`（Cursor 新增的 filter，正确）
- **读不到 `X-Forwarded-Host`** → 回退到请求本身的 Host，即 Auth 服务内网地址 `192.168.1.106:9001`
- 重建后的 `getRequestURL()` 返回 `http://192.168.1.106:9001/auth/login/authenticate` 而非 `http://localhost:9999/auth/login/authenticate`

### 故障链路

```
浏览器 POST http://localhost:9999/auth/login/authenticate     ← 通过网关
  → Gateway StripPrefix /auth，转发到 Auth 服务
    → Auth 服务验证凭证成功
    → OAuthAuthorizeLoginSuccessHandler 触发
      → 从共享 HttpSessionRequestCache 查找 SavedRequest
```

**关键点 1：SavedRequest 中保存的 URL 是 `http://192.168.1.106:9001/auth/oauth2/authorize?client_id=...`**

这是因为 Step 1 中访问 `/oauth2/authorize` 时，SAS 过滤链的 `HttpSessionRequestCache` 保存了经过 `ForwardedHeaderTransformer` 改造后的请求 URL，而该 URL 的 Host 已被重建为 Auth 服务内网地址。

**关键点 2：`OAuthLoginRedirectResolver.normalizeAuthorizeRedirectUrl` 无法正确匹配该 URL**

```java
String issuer = normalizeIssuer();  // "http://localhost:9999/auth"
// savedUrl = "http://192.168.1.106:9001/auth/oauth2/authorize?client_id=..."
// 不匹配 issuer + "/oauth2" → 跳过
// 不以 "/oauth2" 开头 → 跳过（域名开头）
// URI 解析: path = "/auth/oauth2/authorize" → 不匹配 path.startsWith("/oauth2")
// 最后原样返回 "http://192.168.1.106:9001/auth/oauth2/authorize?..."
```

方法最后走到了 `URI.create(redirectUrl)` 分支，但 `path = /auth/oauth2/authorize` 不满足 `path.startsWith("/oauth2")`（因为多了 `/auth` 前缀），**返回原 URL**。因此 `resolvePostLoginRedirectUrl` 返回了 Auth 内网地址的 authorize URL。

**关键点 3：即使返回了内网 authorize URL，浏览器访问该地址时 Session 也可能未正确关联**

浏览器跳转到 `http://192.168.1.106:9001/auth/oauth2/authorize?client_id=...` 后，Auth 服务处理该请求。理想情况下应该因为已持有认证 Session 而直接下发授权码。但实际上：

- 该请求可能因为 Cookie domain/path 不匹配导致 Session 丢失（Auth 内网直连 vs 网关转发，Cookie 的 path 是 `/` 还是包含 `/auth` 存在差异）
- 或者 Session fixation 保护导致认证后的 Session 不是之前保存 SavedRequest 的那个 Session

最终 Auth 服务无法识别为已认证用户，由 `ClientAwareLoginUrlAuthenticationEntryPoint` 再次重定向到 `http://192.168.1.106:9001/login/admin`，页面重新回到登录页。

### 完整链路

```
[浏览器] → GET http://localhost:9999/auth/oauth2/authorize?client_id=...
  → [网关] → ForwardedHeaderTransformer 重建 URL 为 http://192.168.1.106:9001/auth/oauth2/authorize?...
    → [Auth SAS Chain] 保存 SavedRequest (URL = http://192.168.1.106:9001/auth/oauth2/authorize?...) 到共享 requestCache
    → ClientAwareLoginUrlAuthenticationEntryPoint → 302 到 http://localhost:9999/auth/login/admin (使用 issuer 配置)
  → [浏览器] → GET http://localhost:9999/auth/login/admin
    → 渲染登录页，用户输入凭据
    → POST http://localhost:9999/auth/login/authenticate
  → [网关] → 转发到 Auth 服务
    → [Auth 服务] 验证成功
    → [OAuthAuthorizeLoginSuccessHandler] 从共享 requestCache 获取 SavedRequest
      → resolvePostLoginRedirectUrl → normalizeAuthorizeRedirectUrl
      → 返回 http://192.168.1.106:9001/auth/oauth2/authorize?client_id=...
    → response.sendRedirect("http://192.168.1.106:9001/auth/oauth2/authorize?...")
  → [浏览器] → GET http://192.168.1.106:9001/auth/oauth2/authorize?...
    → [Auth 服务] Session 不匹配/丢失 → 未认证
    → ClientAwareLoginUrlAuthenticationEntryPoint → 302 到 http://192.168.1.106:9001/login/admin
```

## 建议修复方案

### 方案一（推荐）：网关补全 `X-Forwarded-Host`

在 Gateway 的 Auth 路由 filter 中增加 `X-Forwarded-Host` 头：

```yaml
filters:
  - StripPrefix=1
  - SetRequestHeader=X-Forwarded-Prefix, /auth
  - SetRequestHeader=X-Forwarded-Host, localhost:9999   # 新增，修复 Host 重建
```

这样 `ForwardedHeaderTransformer` 就能将请求 URL 正确重建为 `http://localhost:9999/auth/...`，SavedRequest 中的 URL 也会是正确的网关地址。

### 方案二：Auth 服务关闭 `forward-headers-strategy`，纯配置驱动 URL 生成

删除 `sca-skeleton-auth-dev.yaml` 中的 `server.forward-headers-strategy: framework`，完全依赖 `sca.auth.issuer=http://localhost:9999/auth` 和 `publicPathPrefix=/auth` 来生成对外 URL，避免 `X-Forwarded-*` 带来的不确定性。

### 方案三（辅助）：增强 `normalizeAuthorizeRedirectUrl` 容错

当 SavedRequest URL 的 Host 非 issuer 时，从 URL 中提取 path 和 query，用 issuer 重新拼接，而不是原样返回。例如：

```java
// 当前代码只处理了 path.startsWith("/oauth2") 的情况
// 但对于 /auth/oauth2/authorize 这类 path 不匹配
// 应提取 /oauth2/authorize?client_id=... 部分并用 issuer 拼接
```

## 补充说明

- `192.168.1.106` 是这台机器的内网 IP
- `9001` 是 Auth 服务的 `server.port`
- `http://localhost:5173/api-dev/auth/oauth2/authorize?client_id=...` 的 500 错误是第二步的**次生故障**——Vite 开发服务器代理 `/api-dev` 到 `http://localhost:9999`，但由于缺少 `/auth` 前缀的路由精确匹配或其他原因导致下游返回 500
- Cursor 新增的 `X-Forwarded-Prefix` 方向是对的，但 `ForwardedHeaderTransformer` 需要完整的 `X-Forwarded-*` 头族才能正确重建 URL
