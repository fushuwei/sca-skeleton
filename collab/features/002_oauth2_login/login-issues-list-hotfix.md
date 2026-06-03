# 登录退出问题修复记录

## 问题 1: /auth/logout 要求经过认证，未登录不允许访问

**原因分析**：

Spring Security 的 `LogoutFilter` 在拦截 `POST /logout` 时，虽然通过 `LogoutConfigurer.permitAll()` 配置为允许匿名访问，但在 Spring Framework 7 + Spring Security 7 的默认过滤链中，`AuthorizationFilter` 仍然可能在此之前的过滤器中检查认证状态。此外，`AuthSessionController` 中仍保留了 `@GetMapping("/logout")` 和 `@PostMapping("/logout")` 方法，与 `LogoutFilter` 形成潜在的 handler mapping 冲突——两个组件都声明自己处理 `/logout`，导致请求路由不确定性。

**修复方案**：

采用双层防护策略：
1. 保留 `LogoutFilter` 的 `.permitAll()` 配置处理 POST /logout
2. 在 `authorizeHttpRequests` 中额外添加 `.requestMatchers("/logout").permitAll()`，确保即使 `LogoutFilter` 因任何原因未拦截请求，`AuthorizationFilter` 也会放行
3. 移除 `AuthSessionController` 中重复的 `@GetMapping("/logout")` 和 `@PostMapping("/logout")` 方法，消除 handler mapping 冲突

**修复状态**：已修复

---

## 问题 2: 刷新 Token 未在 HttpOnly Cookie / Redis 中

**原因分析**：

这是对 PKCE 公共客户端架构的误解，并非 Bug：
- **HttpOnly Cookie 问题**：SPA（单页应用）作为 OAuth2 公共客户端，需要使用 access_token 调用后端 API。HttpOnly Cookie 的 JavaScript 不可读特性导致 SPA 无法获取 token 注入 Authorization 头。因此 PKCE 模式下 token 必须存储在 JavaScript 可访问的位置（localStorage / 内存）。
- **Redis 问题**：刷新 token 实际存储在 Redis 中，由 `RedisOAuth2AuthorizationService` 管理（位于 `sca-skeleton-starter-security` 模块）。存储的 key 格式为 `auth:authorization:<id>`，而非独立的 token key。用户可以通过 `KEYS auth:authorization:*` 在 Redis 0 号库中找到 OAuth2 授权记录，其中包含 access_token 与 refresh_token。
- **安全说明**：退出时后端通过 `OAuth2AuthorizationService.findByToken()` + `remove()` 完成令牌吊销，前端将 token 作为表单参数提交（非 URL 参数），确保销毁完成后即便 token 泄露也无法继续使用，安全性优于纯客户端清除。

**修复状态**：无需修复（设计如此）

---

## 问题 3: Portal 登录验证码错误无提示

**原因分析**：

`CaptchaVerificationFilter` 校验失败后 302 重定向到 `{loginUrl}?error&captcha-error`。后端 `LoginPageController` 正确设置了 `model.addAttribute("captchaError", captchaError != null)`，但 HTML 模板中缺少对应的服务端错误展示区块——当前模板只有前端 JS 表单校验的错误提示，没有渲染服务端 302 重定向后携带的错误参数。

**修复方案**：

1. 在 `admin.html` 和 `portal.html` 模板的表单上方添加服务端错误提示区块，由 Thymeleaf 根据 `loginError` / `captchaError` 模型属性条件渲染
2. 在 `login.js` 的页面初始化阶段，读取 URL 查询参数 `error` 和 `captcha-error`，动态显示对应的错误提示

**修复状态**：已修复

---

## 问题 4: Admin 退出后重新登录跳转到 `?error&reason=oauth_session`

**原因分析**：

退出登录时 `LogoutFilter` 会销毁 Session（包括 `OAuthPendingAuthorizeStore` 保存的 pending authorize URL 和 `HttpSessionRequestCache` 中的 SavedRequest）。用户退出后直接在登录页输入密码登录，此时 `OAuthAuthorizeLoginSuccessHandler.onAuthenticationSuccess()` 的恢复逻辑执行如下：

1. `pendingAuthorizeStore.peekPendingAuthorizeUrl()` → null（已随 Session 销毁）
2. `requestCache.getRequest()` → null（已随 Session 销毁）
3. 进入 fallback：`response.sendRedirect(loginFailureUrl + "&reason=oauth_session")` → 回到登录页显示错误

这个 fallback 逻辑的设计假设是"登录页总是通过 authorize 端点未登录拦截到达"，但用户可能直接访问登录页（如退出后被重定向到登录页），此时确实没有可恢复的 authorize URL。

**修复方案**：

当登录成功后无 authorize URL 可恢复时，不再重定向到登录错误页，而是直接构造一个新的 OAuth2 authorize URL 并 302 跳转。这样 SPA 会自动完成 PKCE → 授权码换 token 的完整流程，用户体验与首次登录一致。

**修复状态**：已修复

---

## 问题 5: 登录过程中的 loading 状态

**原因分析**：

登录页面已经内嵌了 `.page-loader` 加载遮罩（在 `admin.html` / `portal.html` 的 `<div class="page-loader" id="pageLoader">`），CSS 也定义了 `.page-loader` 的动画样式。但是当前 `login.js` 在提交表单时只修改了登录按钮的状态（loading 类 + "登录中..."文字），没有重新显示页面级 loading 遮罩。由于表单提交后会触发整页跳转（302 重定向链），在等待期间用户看到的是一个处于 loading 状态的按钮，体验不够好。

此外，`OAuthCallbackView.vue` 中显示"正在完成登录，请稍候…"的纯文本提示，也没有使用全局 loading 效果。

**修复方案**：

1. `login.js`：表单提交前显示页面级 loading 遮罩（`pageLoader`）
2. `OAuthCallbackView.vue`（admin & portal）：将"正在完成登录…"纯文本替换为带 Logo 的全屏 loading 组件

**修复状态**：已修复

---

## 问题 6: 登录页停留后登录刷新无反应

**原因分析**：

与问题 4 根因相同。用户在登录页停留时间过长（超过 Session 超时时间 30 分钟或 SavedRequest 已被清理），登录成功后 `OAuthAuthorizeLoginSuccessHandler` 无法恢复 authorize URL，fallback 到登录页错误 URL。

**修复方案**：与问题 4 合并修复——在无 authorize URL 时主动发起新的 OAuth2 授权流。

**修复状态**：已修复（与问题 4 合并）

---

## 问题 7: 退出后访问 localhost:5173 跳转到无效回调

**原因分析**：

可能出现以下场景：

1. **浏览器历史记录恢复**：用户退出登录后，浏览器历史中可能保留了 `/oauth/callback?code=...&state=...` 的条目。当用户输入 localhost:5173 时，浏览器可能尝试恢复之前的导航状态。

2. **sessionStorage 被清除**：OAuth callback 依赖 `sessionStorage` 中的 PKCE session（`oauth_pkce_session:{clientId}` 键）完成 code_verifier 校验。如果浏览器刷新、关闭标签页后 `sessionStorage` 被清空，或者 PKCE session 已在之前的回调中被 `consumePkceSession()` 消费，再次访问 callback URL 时就会找不到 PKCE session，显示"授权回调参数无效"。

**修复方案**：

在 `OAuthCallbackView.vue` 的错误分支中，不再仅显示静态错误文本，而是自动触发重新登录流程（带倒计时提示），提升用户体验。

**修复状态**：已修复
