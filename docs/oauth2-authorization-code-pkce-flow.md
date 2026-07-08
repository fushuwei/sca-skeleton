# OAuth 2.1 Authorization Code + PKCE 认证授权流程

> 本文档描述 SCA Skeleton 项目中完整的 OAuth 2.1 授权码 + PKCE 认证与授权流程，涵盖浏览器、SPA（admin/portal）、API 网关、认证中心、资源服务器、Redis、MySQL 之间的所有交互。

## 系统角色

| 角色 | 说明 | 地址 |
|------|------|------|
| Browser | 用户浏览器 | — |
| SPA | 单页应用（admin / portal） | `http://localhost:8080`（admin）或 `http://localhost:9090`（portal） |
| Gateway | Spring Cloud Gateway API 网关 | `http://localhost:9999` |
| Auth Server | Spring Authorization Server 认证中心 | `http://localhost:9001`（内网） |
| Resource Server | 资源服务器（如 system 服务） | 内网 `lb://sca-skeleton-system` |
| Redis | 令牌存储 / Session / 缓存 | `localhost:6379` |
| MySQL | 客户端注册 / 用户 / Consent | `localhost:3306` |

## 关键配置参数

| 参数 | 示例值 | 说明 |
|------|--------|------|
| `issuer` | `http://localhost:8080/auth` | 对外暴露的 OAuth2 根 URL |
| `admin client_id` | `sca-admin-client` | 管理后台 OAuth2 客户端 |
| `portal client_id` | `sca-portal-client` | 前台门户 OAuth2 客户端 |
| `admin redirect_uri` | `http://localhost:8080/oauth/callback`（开发） | admin 回调地址 |
| `portal redirect_uri` | `http://localhost:9090/oauth/callback`（开发） | portal 回调地址 |
| `access_token_ttl` | 900s | 访问令牌有效期 |
| `refresh_token_ttl` | 7200s | 刷新令牌有效期 |
| `authorization_code_ttl` | 60s | 授权码有效期 |

---

## 一、授权码 + PKCE 完整流程

```mermaid
sequenceDiagram
    participant Browser as 🌐 Browser
    participant SPA as 📱 SPA<br/>(admin/portal)
    participant GW as 🚪 API Gateway<br/>:9999
    participant Auth as 🔐 Auth Server<br/>:9001
    participant Redis as 📦 Redis
    participant MySQL as 🗄️ MySQL
    participant RS as 🖥️ Resource Server<br/>(system)

    Note over Browser,RS: ══════ 阶段 1：SPA 发起授权请求（PKCE） ══════

    SPA->>SPA: 生成 code_verifier (43-128 随机字符)<br/>计算 code_challenge = BASE64URL(SHA256(verifier))
    SPA->>SPA: 生成 state (随机字符串，防 CSRF)
    SPA->>Browser: 302 跳转授权端点

    Note over Browser,GW: GET {issuer}/auth/oauth2/authorize?<br/>response_type=code<br/>&client_id=sca-admin-client<br/>&redirect_uri=http://localhost:8080/oauth/callback<br/>&code_challenge={S256_challenge}<br/>&code_challenge_method=S256<br/>&state={random_state}<br/>&scope=openid profile offline_access all

    Note over Browser,RS: ══════ 阶段 2：网关转发 → 认证中心授权端点 ══════

    Browser->>GW: GET /auth/oauth2/authorize?<br/>response_type=code&client_id=sca-admin-client<br/>&redirect_uri=...&code_challenge=...<br/>&code_challenge_method=S256&state=...&scope=...

    Note over GW: GatewaySecurityGlobalFilter<br/>白名单匹配 /auth/oauth2/authorize ✅<br/>免 Bearer Token 校验，直接放行

    GW->>Auth: StripPrefix=1<br/>GET /oauth2/authorize?<br/>response_type=code&client_id=sca-admin-client<br/>&redirect_uri=...&code_challenge=...<br/>&code_challenge_method=S256&state=...&scope=...

    Note over Auth: Order=1 SecurityFilterChain<br/>securityMatcher: /oauth2/**, /.well-known/**<br/>↓<br/>AuthorizeChannelIsolationFilter

    alt 已登录但渠道不匹配 (如 admin Session 访问 portal)
        Auth->>Redis: readPendingMap(session) 暂存 pending authorize
        Auth->>Auth: session.invalidate() + SecurityContextHolder.clearContext()
        Note over Auth: 旧 Session 销毁，强制重新登录<br/>pending map 暂存到 request attribute
    end

    Note over Auth: 未登录 / 刚销毁 Session<br/>↓<br/>ClientAwareLoginUrlAuthenticationEntryPoint

    Auth->>Auth: restorePreservedPendingMap(request)<br/>将暂存的 pending map 写入新 Session
    Auth->>Redis: savePendingAuthorizeRequest(request)<br/>Session.write(SCA_OAUTH2_PENDING_AUTHORIZE_MAP)<br/>key=admin → value={issuer}/oauth2/authorize?...
    Auth->>Auth: 从 client_id=sca-admin-client 解析渠道 → admin<br/>resolveExternalLoginUrl(admin) → {issuer}/login/admin

    Auth->>GW: 302 Location: {issuer}/login/admin
    GW->>Browser: 302 Location: {issuer}/login/admin

    Note over Browser,RS: ══════ 阶段 3：登录页渲染 ══════

    Browser->>GW: GET /auth/login/admin
    Note over GW: GatewaySecurityGlobalFilter<br/>白名单匹配 /auth/login/** ✅<br/>免 Bearer Token 校验，直接放行
    GW->>Auth: StripPrefix=1<br/>GET /login/admin

    Note over Auth: LoginPageController.adminLogin()<br/>@GetMapping("/login/admin")

    Auth->>Redis: peekPendingAuthorizeUrl(request, "admin")
    Redis-->>Auth: {issuer}/oauth2/authorize?... (非空 → 合法 OAuth2 跳转)
    Note over Auth: hasPendingAuthorize = true<br/>正常展示登录页（而非重定向到 SPA）

    Auth->>Auth: 生成 captchaKey = UUID.randomUUID()<br/>渲染 Thymeleaf 模板 login/admin.html
    Note over Auth: Model 注入：<br/>- loginChannel = "admin" (hidden)<br/>- captchaKey = {uuid}<br/>- loginProcessingUrl = "/auth/login/authenticate"<br/>- systemName / logoUrl / copyright / carouselImages

    Auth->>GW: 200 HTML (admin 登录页)
    GW->>Browser: 200 HTML (admin 登录页)

    Note over Browser: 页面包含：<br/>① 验证码图片 &lt;img src="/auth/captcha/generate?key={uuid}"&gt;<br/>② 登录表单 action="/auth/login/authenticate" method="POST"<br/>③ 隐藏域 loginChannel=admin

    Note over Browser,RS: ══════ 阶段 4：验证码图片加载 ══════

    Browser->>GW: GET /auth/captcha/generate?key={uuid}
    Note over GW: 白名单匹配 /auth/captcha/** ✅
    GW->>Auth: StripPrefix=1<br/>GET /captcha/generate?key={uuid}

    Note over Auth: CaptchaController.generate()<br/>@GetMapping("/captcha/generate")

    Auth->>Auth: 创建 SpecCaptcha(width=120, height=44, codeLength=4)<br/>生成 4 位验证码文本
    Auth->>Redis: SET captcha::image:{uuid} = {code.toLowerCase()}<br/>EXPIRE 120s
    Auth->>GW: 200 image/png (验证码图片)
    GW->>Browser: 200 image/png

    Note over Browser,RS: ══════ 阶段 5：表单登录 ══════

    Browser->>GW: POST /auth/login/authenticate<br/>Content-Type: application/x-www-form-urlencoded<br/>Body:<br/>  username=admin<br/>&password=***<br/>&loginChannel=admin<br/>&captchaKey={uuid}<br/>&captchaCode=ABCD

    Note over GW: 白名单匹配 (由 /auth/login/** 覆盖) ✅
    GW->>Auth: StripPrefix=1<br/>POST /login/authenticate<br/>Body: username=admin&password=***<br/>&loginChannel=admin&captchaKey=...&captchaCode=...

    Note over Auth: Order=2 SecurityFilterChain<br/>(排除 /oauth2/** 和 /.well-known/**)<br/>↓

    Note over Auth: ① LoginChannelFilter<br/>(OncePerRequestFilter, POST /login/authenticate)
    Auth->>Auth: 读取 loginChannel=admin<br/>LoginChannelContext.set(LoginChannel.ADMIN)

    Note over Auth: ② CaptchaVerificationFilter<br/>(OncePerRequestFilter, POST /login/authenticate)
    Note over Auth: loginChannel=admin → 非 portal<br/>跳过验证码校验，直接放行

    Note over Auth: ③ UsernamePasswordAuthenticationFilter
    Auth->>Auth: 提取 username=admin, password=***
    Auth->>Auth: RoutingUserDetailsService.loadUserByUsername("admin")
    Auth->>Auth: LoginChannelContext.get() → ADMIN

    alt admin 渠道
        Auth->>MySQL: ScaUserDetailsService.loadUserByUsername("admin")<br/>SELECT * FROM sys_user<br/>WHERE username='admin' AND user_category='backend'
        MySQL-->>Auth: SysUser { userId, username, password, permissions, ... }
    else portal 渠道
        Auth->>MySQL: ScaUserDetailsService.loadFrontendUserByUsername("user")<br/>SELECT * FROM sys_user<br/>WHERE username='user' AND user_category='frontend'
        MySQL-->>Auth: SysUser { userId, username, password, permissions, ... }
    end

    Auth->>Auth: 构建 ScaUserDetails<br/>(userId, username, password, tenantId, userType, nickname, permissions)
    Auth->>Auth: PasswordEncoder.matches(rawPassword, encodedPassword)
    Auth->>Auth: 登录成功 → Authentication 写入 SecurityContext

    Note over Auth: ④ OAuthAuthorizeLoginSuccessHandler.onAuthenticationSuccess()
    Auth->>Redis: Session.setAttribute(SCA_LOGIN_CHANNEL, "admin")
    Auth->>Auth: OAuthLoginRedirectResolver.resolvePostLoginRedirectUrl(request, response, "admin")

    Auth->>Redis: peekPendingAuthorizeUrl(request, "admin")
    Redis-->>Auth: {issuer}/oauth2/authorize?response_type=code<br/>&client_id=sca-admin-client&redirect_uri=...<br/>&code_challenge=...&code_challenge_method=S256&state=...

    Auth->>Auth: 解析到 pending authorize URL ✅
    Auth->>Redis: clearPendingAuthorizeUrl(request, "admin")<br/>removeSavedRequest(request, response)

    Auth->>GW: 302 Location: {issuer}/oauth2/authorize?<br/>response_type=code&client_id=sca-admin-client<br/>&redirect_uri=...&code_challenge=...<br/>&code_challenge_method=S256&state=...&scope=...

    GW->>Browser: 302 Location: {issuer}/oauth2/authorize?...

    Note over Browser,RS: ══════ 阶段 6：授权端点（已登录）→ 生成授权码 ══════

    Browser->>GW: GET /auth/oauth2/authorize?<br/>response_type=code&client_id=sca-admin-client<br/>&redirect_uri=...&code_challenge=...<br/>&code_challenge_method=S256&state=...&scope=...

    Note over GW: 白名单匹配 ✅ → 放行
    GW->>Auth: StripPrefix=1<br/>GET /oauth2/authorize?response_type=code&...

    Note over Auth: Order=1 SAS SecurityFilterChain<br/>① AuthorizeChannelIsolationFilter

    Auth->>Redis: Session.getAttribute(SCA_LOGIN_CHANNEL) → "admin"
    Auth->>Auth: 从 client_id=sca-admin-client 解析预期渠道 → admin<br/>actualChannel("admin") == expectedChannel("admin") ✅<br/>渠道匹配，放行

    Note over Auth: ② SAS OAuth2AuthorizationEndpointFilter
    Auth->>MySQL: RegisteredClientRepository.findByClientId("sca-admin-client")
    MySQL-->>Auth: RegisteredClient {<br/>  clientId=sca-admin-client<br/>  clientAuthenticationMethod=NONE<br/>  authorizationGrantType=AUTHORIZATION_CODE, REFRESH_TOKEN<br/>  redirectUri=http://localhost:8080/oauth/callback<br/>  requireProofKey=true<br/>  requireAuthorizationConsent=false<br/>}

    Note over Auth: 验证：<br/>- response_type=code ✅<br/>- client_id 存在 ✅<br/>- redirect_uri 匹配 ✅<br/>- PKCE required → code_challenge 存在 ✅<br/>- requireAuthorizationConsent=false → 跳过 consent 页

    Auth->>Auth: 生成 authorization_code (OAuth2AuthorizationCode)<br/>有效期 60s
    Auth->>Auth: 构建 OAuth2Authorization 对象<br/>(id, registeredClientId, principalName,<br/>authorizationGrantType, authorizedScopes,<br/>state, authorizationCode, attributes)

    Auth->>Redis: OAuth2AuthorizationService.save(authorization)<br/>① HSET auth:{id} → Hash 存储完整授权记录<br/>② SET idx:state:{state} → {id} (TTL)<br/>③ SET idx:code:{code} → {id} (TTL 60s)
    Note over Redis: authorization_code 仅存于 Redis<br/>不写入 MySQL oauth2_authorization 表

    Auth->>GW: 302 Location: http://localhost:8080/oauth/callback?<br/>code={authorization_code}&state={random_state}
    GW->>Browser: 302 Location: http://localhost:8080/oauth/callback?<br/>code={authorization_code}&state={random_state}

    Note over Browser,RS: ══════ 阶段 7：SPA 回调 → 令牌交换 (PKCE 验证) ══════

    Browser->>SPA: GET /oauth/callback?<br/>code={authorization_code}&state={random_state}

    SPA->>SPA: 验证 state == 原始 state ✅<br/>(防 CSRF 攻击)
    SPA->>SPA: 取出之前存储的 code_verifier

    Note over SPA: 公共客户端 (ClientAuthenticationMethod=NONE)<br/>无需 client_secret，直接 POST token 端点

    SPA->>GW: POST /auth/oauth2/token<br/>Content-Type: application/x-www-form-urlencoded<br/>Body:<br/>  grant_type=authorization_code<br/>&code={authorization_code}<br/>&redirect_uri=http://localhost:8080/oauth/callback<br/>&code_verifier={original_verifier}<br/>&client_id=sca-admin-client

    Note over GW: 白名单匹配 /auth/oauth2/token ✅<br/>(限流: replenishRate=10, burstCapacity=20)
    GW->>Auth: StripPrefix=1<br/>POST /oauth2/token<br/>Body: grant_type=authorization_code&code=...<br/>&redirect_uri=...&code_verifier=...&client_id=...

    Note over Auth: SAS OAuth2TokenEndpointFilter

    Auth->>MySQL: RegisteredClientRepository.findByClientId("sca-admin-client")
    MySQL-->>Auth: RegisteredClient (clientAuthenticationMethod=NONE)

    Auth->>Redis: OAuth2AuthorizationService.findByToken(code, CODE)<br/>① GET idx:code:{code} → authorization id<br/>② HGETALL auth:{id}
    Redis-->>Auth: OAuth2Authorization { authorizationCode, state, ... }

    Note over Auth: PKCE 验证：<br/>BASE64URL(SHA256(code_verifier)) == code_challenge ✅<br/><br/>其他校验：<br/>- authorization_code 未过期 ✅<br/>- redirect_uri 完全一致 ✅<br/>- client_id 匹配 ✅<br/>- code 一次性使用 → 消费后删除

    Auth->>Auth: ScaOpaqueAccessTokenClaimsCustomizer.customize()<br/>从 ScaUserDetails 提取业务 claims：<br/>- sub = userId<br/>- preferred_username = username<br/>- tenant_id = tenantId<br/>- user_type = userType<br/>- nickname = nickname<br/>- permissions = [p1, p2, ...]

    Auth->>Auth: 生成 access_token (不透明/REFERENCE 格式)<br/>有效期 900s<br/>metadata 中包含业务 claims

    Auth->>Auth: ScaRefreshTokenGenerator.generate()<br/>生成 refresh_token (96 字节随机字符串)<br/>有效期 7200s，启用 rotation

    Auth->>Redis: OAuth2AuthorizationService.save(authorization)<br/>更新 Redis Hash：<br/>- access_token_value / access_token_issued_at /<br/>  access_token_expires_at / access_token_metadata<br/>- refresh_token_value / refresh_token_issued_at /<br/>  refresh_token_expires_at<br/>- 建索引 idx:access:{token} → id (TTL 900s)<br/>- 建索引 idx:refresh:{token} → id (TTL 7200s)<br/>- 删除旧 code 索引
    Auth->>Redis: 删除旧 code 索引 (authorization_code 已消费)

    Auth->>GW: 200 OK<br/>Content-Type: application/json<br/>Body:<br/>{<br/>  "access_token": "{opaque_token}",<br/>  "refresh_token": "{refresh_token}",<br/>  "token_type": "Bearer",<br/>  "expires_in": 900,<br/>  "scope": "openid profile offline_access all"<br/>}

    GW->>SPA: 200 OK<br/>{ access_token, refresh_token, ... }

    SPA->>SPA: 存储 access_token / refresh_token<br/>(localStorage / sessionStorage / memory)<br/>路由跳转到应用主页

    Note over Browser,RS: ══════ 阶段 8：资源服务器 API 访问（Bearer Token） ══════

    SPA->>GW: GET /sys/api/users/me<br/>Authorization: Bearer {access_token}

    Note over GW: GatewaySecurityGlobalFilter<br/>请求路径 /sys/api/users/me<br/>不在白名单 → 需要 Bearer Token

    GW->>GW: BearerTokenUtils.extractBearerToken()<br/>提取 token = "{access_token}" ✅<br/>非空 → 放行

    GW->>RS: GET /api/users/me<br/>Authorization: Bearer {access_token}

    Note over RS: ResourceServerAutoConfiguration<br/>SecurityFilterChain<br/>SessionCreationPolicy.STATELESS<br/>CSRF disabled

    Note over RS: BearerTokenAuthenticationFilter<br/>提取 Bearer token → 自省

    RS->>RS: RedisOpaqueTokenIntrospector.introspect(token)

    RS->>RS: RedisOpaqueTokenIntrospector<br/>.extractAccessTokenClaims(authorizationService, token)<br/>① GET idx:access:{token} → authorization id<br/>② HGETALL auth:{id}

    Redis-->>RS: access_token_value, access_token_expires_at,<br/>access_token_metadata (含业务 claims)

    RS->>RS: 校验 token 未过期 ✅<br/>提取 claims：<br/>- sub = {userId}<br/>- preferred_username = {username}<br/>- permissions = [p1, p2, ...]<br/>- tenant_id / user_type / nickname

    RS->>RS: 构建 RedisOAuth2AuthenticatedPrincipal<br/>(principalName, claims, authorities)<br/>claims.active = true

    RS->>RS: DefaultOpaqueTokenAuthenticationConverter<br/>.convert(token, principal)<br/>构建 BearerTokenAuthentication

    Note over RS: SecurityContext 设置 Authentication

    alt @PreAuthorize 方法级鉴权
        RS->>RS: @PreAuthorize("hasAuthority('user:read')")<br/>检查 GrantedAuthority 列表
    end

    alt 权限通过
        RS->>RS: 执行 Controller 业务逻辑<br/>CurrentUserProviderImpl 从 BearerTokenAuthentication<br/>读取当前用户信息
        RS->>GW: 200 OK + 业务数据
        GW->>SPA: 200 OK + 业务数据
    else 权限不足
        RS->>GW: SecurityAccessDeniedHandler<br/>403 JSON { code: 403, msg: "Access Denied" }
        GW->>SPA: 403 Forbidden
    end

    Note over Browser,RS: ══════ 阶段 9：令牌刷新（Refresh Token Rotation） ══════

    Note over SPA: access_token 即将过期 (或已过期收到 401)

    SPA->>GW: POST /auth/oauth2/token<br/>Content-Type: application/x-www-form-urlencoded<br/>Body:<br/>  grant_type=refresh_token<br/>&refresh_token={current_refresh_token}<br/>&client_id=sca-admin-client

    Note over GW: 白名单匹配 ✅
    GW->>Auth: StripPrefix=1<br/>POST /oauth2/token<br/>Body: grant_type=refresh_token&refresh_token=...

    Auth->>Redis: OAuth2AuthorizationService.findByToken<br/>(refresh_token, REFRESH_TOKEN)
    Redis-->>Auth: OAuth2Authorization (含旧 refresh_token)

    Auth->>Auth: 验证 refresh_token 未过期 ✅<br/>reuseRefreshTokens=false → 旋转刷新

    Auth->>Auth: 生成新的 access_token (不透明)<br/>ScaOpaqueAccessTokenClaimsCustomizer 写入 claims
    Auth->>Auth: 生成新的 refresh_token (ScaRefreshTokenGenerator)<br/>(旧 refresh_token 将被废弃)

    Auth->>Redis: OAuth2AuthorizationService.save(authorization)<br/>更新 access_token / refresh_token<br/>旧 refresh_token 索引被 removeIndexes 清除

    Auth->>GW: 200 OK<br/>{<br/>  "access_token": "{new_opaque_token}",<br/>  "refresh_token": "{new_rotated_token}",<br/>  "token_type": "Bearer",<br/>  "expires_in": 900,<br/>  "scope": "openid profile offline_access all"<br/>}

    GW->>SPA: 200 OK (新令牌)

    SPA->>SPA: 更新存储的 access_token / refresh_token

    Note over Browser,RS: ══════ 阶段 10：退出登录 ══════

    SPA->>GW: POST /auth/logout<br/>Content-Type: application/x-www-form-urlencoded<br/>Body:<br/>  access_token={current_access_token}<br/>&refresh_token={current_refresh_token}<br/>&channel=admin

    Note over GW: 白名单匹配 /auth/logout ✅
    GW->>Auth: StripPrefix=1<br/>POST /logout<br/>Body: access_token=...&refresh_token=...&channel=admin

    Note over Auth: AuthSecurityConfig LogoutFilter<br/>① revokeTokens()

    Auth->>Redis: OAuth2AuthorizationService.findByToken<br/>(access_token, ACCESS_TOKEN)
    Redis-->>Auth: OAuth2Authorization { id }
    Auth->>Redis: OAuth2AuthorizationService.remove(authorization)<br/>DEL auth:{id} (主 Hash)<br/>DEL idx:access:{token}<br/>DEL idx:refresh:{token}<br/>DEL 其他索引

    Auth->>Redis: OAuth2AuthorizationService.findByToken<br/>(refresh_token, REFRESH_TOKEN)
    Note over Auth: 如果 access 和 refresh 指向同一 authorization<br/>则跳过（已删除，去重逻辑）

    Note over Auth: ② onLogoutSuccess()
    Auth->>Auth: session.invalidate() (销毁 Session)
    Auth->>Auth: SecurityContextHolder.clearContext()

    Auth->>Auth: channel=admin → resolveClientId → sca-admin-client<br/>resolveExternalLoginUrl("sca-admin-client")<br/>→ {issuer}/login/admin

    Auth->>GW: 302 Location: {issuer}/login/admin
    GW->>Browser: 302 Location: {issuer}/login/admin
    Browser->>SPA: 浏览器重定向到登录页
```

---

## 二、令牌自省（资源服务器本地自省，不走 HTTP /oauth2/introspect）

```mermaid
sequenceDiagram
    participant RS as 🖥️ Resource Server
    participant Redis as 📦 Redis

    Note over RS: BearerTokenAuthenticationFilter<br/>提取 Bearer token

    RS->>RS: RedisOpaqueTokenIntrospector.introspect(token)

    RS->>Redis: ① GET idx:access:{token}
    Redis-->>RS: authorization id

    alt token 无效或已过期
        Redis-->>RS: null
        RS->>RS: throw BadOpaqueTokenException<br/>→ SecurityAuthenticationEntryPoint<br/>→ 401 Unauthorized JSON
    end

    RS->>Redis: ② HGETALL auth:{id}
    Redis-->>RS: access_token_value, access_token_expires_at,<br/>access_token_metadata (JSON)

    RS->>RS: 校验 token 过期时间
    Note over RS: Instant.now() vs access_token_expires_at

    RS->>RS: 解析 access_token_metadata JSON<br/>提取 CLAIMS_METADATA_NAME → Map&lt;String, Object&gt;

    Note over RS: claims 结构：<br/>{<br/>  "sub": "user-uuid",<br/>  "preferred_username": "admin",<br/>  "tenant_id": "tenant-001",<br/>  "user_type": "backend",<br/>  "nickname": "管理员",<br/>  "permissions": ["user:read", "user:write", ...]<br/>}

    RS->>RS: 补齐 RFC 7662 语义：<br/>ACTIVE = true<br/>principalName = sub ?? preferred_username

    RS->>RS: 构建 RedisOAuth2AuthenticatedPrincipal<br/>(principalName, claims, authorities)

    Note over RS: DefaultOpaqueTokenAuthenticationConverter<br/>.convert(token, principal)

    RS->>RS: 从 claims 提取 permissions<br/>→ List&lt;SimpleGrantedAuthority&gt;<br/>(无 ROLE_ 前缀，供 hasAuthority 使用)

    RS->>RS: 构建 BearerTokenAuthentication<br/>(principal, accessToken, authorities)
```

---

## 三、Session 与 CSRF 配置

### 3.1 两条 SecurityFilterChain

| Order | 匹配路径 | 用途 | Session | CSRF |
|-------|----------|------|---------|------|
| 1 | `/oauth2/**`, `/.well-known/**` | SAS 标准端点 | IF_REQUIRED | SAS 默认（state 参数提供 CSRF 保护） |
| 2 | 其余 | 表单登录、退出、静态资源 | IF_REQUIRED | 忽略 `/logout`、`/login/authenticate` |

### 3.2 CSRF 设计决策

- **`/login/authenticate`** 忽略 CSRF：admin/portal 可能共享 Session，跨标签页登录时 CSRF token 会冲突。已通过 OAuth2 `state` 参数提供等价保护。
- **`/logout`** 忽略 CSRF：前端通过表单 POST 携带 `access_token` / `refresh_token` 参数，令牌本身就是退出凭证。
- 其余 POST/PUT/DELETE 请求仍需有效 CSRF token。

### 3.3 Session 属性

| Session Key | 类型 | 说明 |
|-------------|------|------|
| `SCA_LOGIN_CHANNEL` | String | 当前登录渠道 (admin/portal) |
| `SCA_OAUTH2_PENDING_AUTHORIZE_MAP` | Map&lt;String, String&gt; | 待恢复的 authorize URL (按渠道隔离) |
| `SPRING_SECURITY_SAVED_REQUEST` | SavedRequest | SAS/表单登录共享的缓存请求 |

---

## 四、Redis 数据结构

### 4.1 OAuth2Authorization Hash

```
Key:   auth:{authorization_id}
Type:  Hash
TTL:   max(access_token_ttl, refresh_token_ttl)

Fields (与 JDBC oauth2_authorization 表对齐):
  id, registered_client_id, principal_name,
  authorization_grant_type, authorized_scopes, attributes, state,
  authorization_code_value, authorization_code_issued_at, authorization_code_expires_at,
  authorization_code_metadata,
  access_token_value, access_token_issued_at, access_token_expires_at,
  access_token_metadata, access_token_type, access_token_scopes,
  oidc_id_token_value, oidc_id_token_issued_at, oidc_id_token_expires_at,
  oidc_id_token_metadata,
  refresh_token_value, refresh_token_issued_at, refresh_token_expires_at,
  refresh_token_metadata,
  user_code_value, user_code_issued_at, user_code_expires_at, user_code_metadata,
  device_code_value, device_code_issued_at, device_code_expires_at, device_code_metadata
```

### 4.2 令牌索引

| Key Pattern | Value | TTL |
|-------------|-------|-----|
| `idx:state:{state}` | authorization_id | fallback TTL |
| `idx:code:{code}` | authorization_id | 60s |
| `idx:access:{token}` | authorization_id | 900s |
| `idx:refresh:{token}` | authorization_id | 7200s |
| `idx:id_token:{token}` | authorization_id | token TTL |
| `idx:user_code:{code}` | authorization_id | token TTL |
| `idx:device_code:{code}` | authorization_id | token TTL |

### 4.3 验证码

```
Key:   captcha::image:{captchaKey}
Value: {code_lowercase}
TTL:   120s
```

### 4.4 登录锁定

```
Key:   login:failed:{username}:{channel}
Value: {failCount}
TTL:   1800s (lockDurationSeconds)
```

---

## 五、关键注解与配置速查

### 5.1 RegisteredClient 配置

```java
ClientAuthenticationMethod.NONE          // 公共客户端，无 client_secret
AuthorizationGrantType.AUTHORIZATION_CODE // 授权码模式
AuthorizationGrantType.REFRESH_TOKEN      // 支持刷新令牌
requireProofKey(true)                     // 强制 PKCE
requireAuthorizationConsent(false)        // 跳过 consent 页
accessTokenFormat(REFERENCE)              // 不透明令牌
reuseRefreshTokens(false)                 // 刷新令牌旋转
authorizationCodeTimeToLive(60s)          // 授权码 60s 有效期
```

### 5.2 Token 自定义

| 组件 | 作用 |
|------|------|
| `ScaOpaqueAccessTokenClaimsCustomizer` | 在 access_token metadata 中写入 userId、username、permissions 等业务 claims |
| `ScaRefreshTokenGenerator` | 替代 SAS 内置生成器，允许向公共客户端签发 refresh_token |

### 5.3 自定义 Filter 链

| Filter | Order=1 (SAS) | Order=2 (表单) | 作用 |
|--------|:---:|:---:|------|
| `AuthorizeChannelIsolationFilter` | ✅ | — | admin/portal 会话隔离 |
| `LoginChannelFilter` | — | ✅ | 解析 loginChannel 表单字段 |
| `CaptchaVerificationFilter` | — | ✅ | portal 渠道验证码校验 |

### 5.4 Handler

| Handler | 触发场景 |
|---------|----------|
| `ClientAwareLoginUrlAuthenticationEntryPoint` | 未登录访问 `/oauth2/authorize`，按 client_id 跳转登录页 |
| `OAuthAuthorizeLoginSuccessHandler` | 表单登录成功，恢复 pending authorize 并重定向 |
| `ChannelAwareAuthenticationFailureHandler` | 表单登录失败，按渠道回跳登录页 + ?error |

---

## 六、网关白名单

网关对以下路径跳过 Bearer Token 校验：

```
/auth/oauth2/authorize      — 授权端点（限流: 20/40 per sec）
/auth/oauth2/token          — 令牌端点（限流: 10/20 per sec）
/auth/oauth2/revoke         — 吊销端点
/auth/oauth2/jwks           — JWKS 端点
/auth/.well-known/**        — OIDC Discovery
/auth/login/**              — 登录页与表单提交
/auth/logout                — 退出端点
/auth/captcha/**            — 验证码图片
/auth/css/**                — 静态资源
/auth/js/**                 — 静态资源
/auth/images/**             — 静态资源
/actuator/health            — 健康检查
/actuator/info              — 应用信息
```

---

## 七、端点总览

| 端点 | 方法 | Content-Type | 关键参数 | 说明 |
|------|------|-------------|----------|------|
| `/auth/oauth2/authorize` | GET | — | `response_type`, `client_id`, `redirect_uri`, `code_challenge`, `code_challenge_method`, `state`, `scope` | 发起授权 |
| `/auth/login/admin` | GET | — | — | admin 登录页 |
| `/auth/login/portal` | GET | — | — | portal 登录页 |
| `/auth/captcha/generate` | GET | — | `key` | 验证码图片 |
| `/auth/login/authenticate` | POST | `application/x-www-form-urlencoded` | `username`, `password`, `loginChannel`, `captchaKey`, `captchaCode` | 表单登录 |
| `/auth/oauth2/token` | POST | `application/x-www-form-urlencoded` | `grant_type`, `code`, `redirect_uri`, `code_verifier`, `client_id` (或 `grant_type=refresh_token`, `refresh_token`, `client_id`) | 令牌交换/刷新 |
| `/auth/oauth2/revoke` | POST | `application/x-www-form-urlencoded` | `token`, `token_type_hint`, `client_id` | 吊销令牌 |
| `/auth/oauth2/jwks` | GET | — | — | JWKS 公钥 |
| `/auth/.well-known/openid-configuration` | GET | — | — | OIDC Discovery |
| `/auth/logout` | POST | `application/x-www-form-urlencoded` | `access_token`, `refresh_token`, `channel` | 退出登录 |
| `/auth/actuator/health` | GET | — | — | 健康检查 |
