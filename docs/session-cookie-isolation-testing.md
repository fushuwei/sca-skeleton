# Session Cookie 隔离修复 - 测试指南

## 问题描述

### 原始问题场景

**场景1（正常工作）**：
1. 访问 admin，出现 admin 登录页，点击登录 → 成功
2. 再访问 portal，出现 portal 登录页，点击登录 → 成功

**场景2（存在问题）**：
1. 访问 admin，出现 admin 登录页（此时不登录）
2. 新开 tab 页访问 portal，出现 portal 登录页（此时也不登录）
3. 回到 admin 登录页点击登录 → 成功
4. 切换到 portal 登录页点击登录 → **直接报错（CSRF 403）**

### 根本原因

admin 和 portal 共享同一个 `JSESSIONID` cookie。当 admin 登录成功后，session 中设置了 `LOGIN_CHANNEL = "admin"`。当 portal 尝试登录时，由于共享同一 session，`AuthorizeChannelIsolationFilter` 检测到渠道不匹配，调用 `session.invalidate()` 销毁 session，导致 CSRF token 失效，返回 403 错误。

## 解决方案

采用 **Session Cookie 名称隔离**方案：
- Admin 使用 `JSESSIONID_ADMIN` cookie
- Portal 使用 `JSESSIONID_PORTAL` cookie

实现同一浏览器中两个系统的 Session 完全隔离，避免跨标签页登录冲突。

## 代码修改

### 1. 新增 SessionCookieResolutionFilter

**文件**: `sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/security/filter/SessionCookieResolutionFilter.java`

**功能**: 
- 在请求处理前，从 `JSESSIONID_ADMIN` 或 `JSESSIONID_PORTAL` 中提取 session ID
- 将其设置为标准的 `JSESSIONID` cookie 供 Spring Session 使用

```java
@Component
public class SessionCookieResolutionFilter extends OncePerRequestFilter {
    // 从渠道特定的 cookie 中读取 session ID
    // 转换为标准 JSESSIONID 供 Spring Session 使用
}
```

### 2. 修改 OAuthAuthorizeLoginSuccessHandler

**文件**: `sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/web/OAuthAuthorizeLoginSuccessHandler.java`

**修改内容**:
- 添加 `setChannelAwareSessionCookie()` 方法
- 添加 `determineCookieName()` 方法
- 在 `onAuthenticationSuccess()` 中调用新方法设置渠道特定的 session cookie

```java
private void setChannelAwareSessionCookie(HttpServletResponse response, String sessionId, String loginChannel) {
    String cookieName = determineCookieName(loginChannel);
    
    // 设置渠道特定的 session cookie
    Cookie channelCookie = new Cookie(cookieName, sessionId);
    channelCookie.setPath("/");
    channelCookie.setHttpOnly(true);
    channelCookie.setMaxAge(-1);
    response.addCookie(channelCookie);
    
    // 删除默认的 JSESSIONID cookie
    Cookie defaultCookie = new Cookie("JSESSIONID", null);
    defaultCookie.setPath("/");
    defaultCookie.setHttpOnly(true);
    defaultCookie.setMaxAge(0);
    response.addCookie(defaultCookie);
}
```

### 3. 更新 AuthSecurityConfig

**文件**: `sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/config/AuthSecurityConfig.java`

**修改内容**:
- 注入 `SessionCookieResolutionFilter`
- 在 Security 过滤链中添加该过滤器（在 `UsernamePasswordAuthenticationFilter` 之前）

```java
private final SessionCookieResolutionFilter sessionCookieResolutionFilter;

.addFilterBefore(sessionCookieResolutionFilter, UsernamePasswordAuthenticationFilter.class)
.addFilterBefore(loginChannelFilter, UsernamePasswordAuthenticationFilter.class)
.addFilterBefore(captchaVerificationFilter, UsernamePasswordAuthenticationFilter.class)
```

### 4. 修复 LoginPageController

**文件**: `sca-skeleton-backend/sca-skeleton-auth/src/main/java/io/github/fushuwei/scaskeleton/auth/web/LoginPageController.java`

**修改内容**:
- 调用 `resolvePostLoginRedirectUrl()` 时传入 channel 参数

```java
String channel = (String) request.getSession(false).getAttribute(AuthSessionAttributes.LOGIN_CHANNEL);
String target = redirectResolver.resolvePostLoginRedirectUrl(request, response, channel);
```

## 编译与部署

### 编译项目

```bash
cd /Users/fuwei/Documents/Workspace/work/sca-skeleton
mvn clean package -pl sca-skeleton-backend/sca-skeleton-auth -am -DskipTests
```

预期输出：
```
[INFO] BUILD SUCCESS
[INFO] Total time: ~3s
```

### 启动基础设施

确保以下服务已启动：

```bash
# 启动 Nacos、Redis、MySQL
docker-compose up -d nacos redis mysql

# 或使用你项目的启动脚本
```

### 启动 Auth 服务

```bash
cd /Users/fuwei/Documents/Workspace/work/sca-skeleton
java -jar sca-skeleton-backend/sca-skeleton-auth/target/sca-skeleton-auth-1.0.0.jar \
  --spring.profiles.active=dev
```

验证服务启动：
```bash
curl http://localhost:9001/actuator/health
# 预期输出: {"status":"UP"}
```

## 测试步骤

### 前置条件

1. Auth 服务已启动并运行在 `http://localhost:9001`
2. Gateway 已启动并运行在 `http://localhost:9999`
3. Admin 前端已启动并运行在 `http://localhost:5173`
4. Portal 前端已启动并运行在 `http://localhost:5174`
5. 数据库中有测试用户：
   - Admin 用户: `admin` / `password`
   - Portal 用户: `portal` / `password`

### 测试场景 1：正常登录流程（基线测试）

**步骤**:
1. 打开浏览器，访问 `http://localhost:5173/login`
2. 输入 admin 用户名和密码，点击登录
3. 验证：登录成功，跳转到 admin dashboard
4. 新开一个标签页，访问 `http://localhost:5174/login`
5. 输入 portal 用户名和密码，点击登录
6. 验证：登录成功，跳转到 portal dashboard

**预期结果**: ✅ 两个系统都能正常登录

### 测试场景 2：跨标签页登录（关键测试）

这是之前存在问题的场景，现在应该被修复。

**步骤**:
1. 打开浏览器 Tab 1，访问 `http://localhost:5173/login`
   - **不要登录**，停留在登录页
2. 打开浏览器 Tab 2，访问 `http://localhost:5174/login`
   - **不要登录**，停留在登录页
3. 切换回 Tab 1 (admin)，输入 admin 用户名和密码，点击登录
4. 验证：admin 登录成功，跳转到 admin dashboard
5. 切换到 Tab 2 (portal)，输入 portal 用户名和密码，点击登录
6. 验证：portal 登录成功，跳转到 portal dashboard

**预期结果**: ✅ 两个系统都能成功登录，不再出现 403 错误

**之前的问题**: 第 6 步会报 CSRF 403 错误  
**现在的表现**: 应该能正常登录

### 测试场景 3：反向跨标签页登录

**步骤**:
1. 打开浏览器 Tab 1，访问 `http://localhost:5174/login` (portal)
   - **不要登录**
2. 打开浏览器 Tab 2，访问 `http://localhost:5173/login` (admin)
   - **不要登录**
3. 切换回 Tab 1 (portal)，输入 portal 用户名和密码，点击登录
4. 验证：portal 登录成功
5. 切换到 Tab 2 (admin)，输入 admin 用户名和密码，点击登录
6. 验证：admin 登录成功

**预期结果**: ✅ 两个系统都能成功登录

### 测试场景 4：同时保持登录状态

**步骤**:
1. 在 Tab 1 登录 admin
2. 在 Tab 2 登录 portal
3. 刷新 Tab 1 (admin)
4. 验证：admin 仍然保持登录状态
5. 刷新 Tab 2 (portal)
6. 验证：portal 仍然保持登录状态

**预期结果**: ✅ 两个系统可以同时保持登录状态，互不影响

### 测试场景 5：独立退出

**步骤**:
1. 在 Tab 1 登录 admin
2. 在 Tab 2 登录 portal
3. 在 Tab 1 执行 admin 退出操作
4. 验证：admin 退出成功，跳转到 admin 登录页
5. 刷新 Tab 2 (portal)
6. 验证：portal 仍然保持登录状态（未受影响）

**预期结果**: ✅ 退出一个系统不影响另一个系统

## 验证 Session Cookie 隔离

### 使用浏览器开发者工具检查 Cookies

1. 打开 Chrome DevTools (F12)
2. 切换到 **Application** 标签
3. 左侧选择 **Cookies** → `http://localhost:9999`
4. 观察 Cookie 列表

**Admin 登录后应该看到**:
```
Name: JSESSIONID_ADMIN
Value: <session-id>
Path: /
HttpOnly: ✓
Secure: (取决于配置)
```

**Portal 登录后应该看到**:
```
Name: JSESSIONID_PORTAL
Value: <session-id>
Path: /
HttpOnly: ✓
Secure: (取决于配置)
```

**不应该看到**:
```
Name: JSESSIONID  ← 这个应该被删除或不存在
```

### 检查 Network 请求

1. 打开 Chrome DevTools (F12)
2. 切换到 **Network** 标签
3. 执行登录操作
4. 查看登录请求的 Response Headers

**Admin 登录响应应该包含**:
```
Set-Cookie: JSESSIONID_ADMIN=<session-id>; Path=/; HttpOnly
Set-Cookie: JSESSIONID=; Path=/; HttpOnly; Max-Age=0
```

**Portal 登录响应应该包含**:
```
Set-Cookie: JSESSIONID_PORTAL=<session-id>; Path=/; HttpOnly
Set-Cookie: JSESSIONID=; Path=/; HttpOnly; Max-Age=0
```

## 常见问题排查

### 问题 1: 编译失败

**错误信息**: `找不到符号` 或 `BUILD FAILURE`

**解决方法**:
```bash
# 清理并重新编译
mvn clean compile -pl sca-skeleton-backend/sca-skeleton-auth -am -DskipTests
```

### 问题 2: Nacos 连接失败

**错误信息**: `Connection refused: /127.0.0.1:8848`

**解决方法**:
```bash
# 启动 Nacos
docker run -d --name nacos -p 8848:8848 -p 9848:9848 nacos/nacos-server:latest

# 或检查 Nacos 是否已在运行
docker ps | grep nacos
```

### 问题 3: Redis 连接失败

**错误信息**: `Cannot get Jedis connection`

**解决方法**:
```bash
# 启动 Redis
docker run -d --name redis -p 6379:6379 redis:latest

# 验证 Redis 运行状态
docker ps | grep redis
```

### 问题 4: 登录后仍然出现 403 错误

**可能原因**: 
- 浏览器缓存了旧的 cookie
- 新的 auth 服务未正确启动

**解决方法**:
```bash
# 1. 清除浏览器 cookies
# 在 Chrome DevTools → Application → Cookies → 右键 → Clear

# 2. 重启 auth 服务
pkill -f sca-skeleton-auth
java -jar sca-skeleton-backend/sca-skeleton-auth/target/sca-skeleton-auth-1.0.0.jar

# 3. 使用无痕模式测试
# Chrome: Ctrl+Shift+N (Windows/Linux) 或 Cmd+Shift+N (Mac)
```

### 问题 5: Session 无法持久化

**可能原因**: Spring Session Redis 配置问题

**检查方法**:
```bash
# 连接到 Redis，检查 session 数据
redis-cli
KEYS sca:auth:session:*
```

**预期输出**:
```
1) "sca:auth:session:sessions:<session-id>"
2) "sca:auth:session:expirations:<timestamp>"
```

## 回滚方案

如果新方案出现问题，可以通过以下步骤回滚：

### 1. 恢复代码

```bash
cd /Users/fuwei/Documents/Workspace/work/sca-skeleton
git checkout HEAD~1 -- sca-skeleton-backend/sca-skeleton-auth/
```

### 2. 重新编译

```bash
mvn clean package -pl sca-skeleton-backend/sca-skeleton-auth -am -DskipTests
```

### 3. 重启服务

```bash
pkill -f sca-skeleton-auth
java -jar sca-skeleton-backend/sca-skeleton-auth/target/sca-skeleton-auth-1.0.0.jar
```

## 技术细节

### Session Cookie 生命周期

- **Max-Age = -1**: Session cookie，浏览器关闭时自动删除
- **HttpOnly = true**: 防止 JavaScript 访问，提高安全性
- **Path = /**: 对整个应用有效

### Spring Session 集成

Spring Session 会自动将 session 数据存储到 Redis 中：
- Key 格式: `sca:auth:session:sessions:<session-id>`
- 过期时间: 30 分钟（可配置）

### 渠道识别逻辑

1. **登录时**: 从表单参数 `loginChannel` 获取
2. **登录后**: 从 Session 属性 `SCA_LOGIN_CHANNEL` 获取
3. **优先级**: Session 属性 > 请求参数

## 总结

本次修复通过为 admin 和 portal 分配独立的 session cookie 名称，彻底解决了跨标签页登录冲突问题。

**核心优势**:
- ✅ 完全隔离：两个系统的 session 互不影响
- ✅ 用户体验：可以同时保持多个系统的登录状态
- ✅ 安全性高：符合安全最佳实践
- ✅ 代码简洁：只添加了 1 个过滤器，修改了少量代码

**测试重点**:
- 场景 2（跨标签页登录）是核心测试用例
- 务必检查浏览器 cookies 确认隔离生效
- 验证退出操作的独立性

---

**作者**: Fu Wei  
**日期**: 2026-06-03  
**版本**: 1.0
