# Session 隔离问题分析与解决方案

## 问题描述

### 场景复现

**场景2（存在问题）**：
1. Tab 1: 访问 admin 登录页（不登录）
2. Tab 2: 访问 portal 登录页（不登录）
3. Tab 1: admin 登录 → 成功
4. Tab 2: portal 登录 → **失败（404或403错误）**

## 根本原因分析

### 1. Session 共享问题

admin 和 portal 共享同一个 `JSESSIONID` cookie。当 admin 登录成功后：
- Session 中设置 `LOGIN_CHANNEL = "admin"`
- 浏览器持有该 JSESSIONID

当 portal 尝试登录时：
- 使用相同的 JSESSIONID
- `AuthorizeChannelIsolationFilter` 检测到渠道不匹配
- 调用 `session.invalidate()` 销毁 session
- CSRF token 失效 → 403 错误

### 2. 404 错误可能原因

如果看到 `{"code":40400,"message":"请求的资源不存在"}` 错误，可能是：

1. **前端API路径配置问题**：
   - Admin 使用 `/api` 作为 base URL
   - Vite 代理将 `/api` 转发到网关并去掉前缀
   - 如果后端服务未正确启动或路由配置错误，会返回404

2. **System服务未启动**：
   - 登录后前端会调用 `/sys/user/profile` 获取用户信息
   - 如果 system 服务未运行，会返回404

3. **网关路由配置问题**：
   - 检查 `sca-skeleton-gateway-dev.yaml` 中的路由配置
   - 确保 `/sys/**` 路径正确路由到 system 服务

## 推荐解决方案

### 方案A：Session Cookie 隔离（推荐，但需要完整重启环境）

**核心思路**：为 admin 和 portal 使用不同的 session cookie 名称
- Admin: `JSESSIONID_ADMIN`
- Portal: `JSESSIONID_PORTAL`

**实现步骤**：

1. **配置 Spring Session CookieSerializer**（需要 Spring Session 3.x+ API支持）
2. **修改登录成功处理器**，设置正确的 cookie 名称
3. **添加 session ID 解析过滤器**，从不同 cookie 中提取 session ID

**优点**：
- 彻底解决跨标签页冲突
- 用户体验好，可同时保持登录状态

**缺点**：
- 需要 Spring Session 版本支持
- 需要重启所有服务

### 方案B：优化 AuthorizeChannelIsolationFilter（快速修复）

**核心思路**：不销毁 session，而是允许临时共存，只在 authorize 端点做严格检查

**实现步骤**：

1. 修改 `AuthorizeChannelIsolationFilter`，不在渠道不匹配时销毁 session
2. 改为在响应头中标记渠道冲突，由前端处理
3. 或者仅在 authorize 请求时做检查，登录表单提交时不做检查

**优点**：
- 改动小，风险低
- 不需要修改 session 管理机制

**缺点**：
- 不能完全解决 session 共享问题
- 可能存在安全隐患

### 方案C：前端层面避免（临时方案）

**核心思路**：在前端检测多标签页登录情况，提示用户

**实现步骤**：

1. 使用 `localStorage` 或 `BroadcastChannel` API 检测其他标签页的登录状态
2. 如果检测到其他渠道已登录，提示用户先退出
3. 或者自动刷新页面以获取最新 session 状态

**优点**：
- 无需修改后端代码
- 快速实施

**缺点**：
- 用户体验差
- 治标不治本

## 当前建议

由于你遇到了 404 错误，这很可能不是 session 隔离的问题，而是：

1. **System 服务未启动** - 请检查 system 服务是否正常运行
2. **网关路由配置问题** - 请检查网关是否正确路由到 system 服务
3. **前端API路径配置** - 请确认 `.env.development` 中的配置正确

### 立即排查步骤

```bash
# 1. 检查所有服务是否运行
lsof -ti:9001  # auth service
lsof -ti:9002  # system service (假设)
lsof -ti:9999  # gateway

# 2. 测试 system 服务
curl http://localhost:9999/sys/user/profile \
  -H "Authorization: Bearer YOUR_TOKEN"

# 3. 检查网关日志
tail -f sca-skeleton-backend/sca-skeleton-gateway/target/*.log

# 4. 检查前端控制台 Network 标签
# 查看哪个具体请求返回了 404
```

### 如果确认是 session 隔离问题

建议采用**方案A（Session Cookie 隔离）**，但需要：

1. 确保 Nacos、Redis、MySQL 都已启动
2. 重新编译 auth 服务
3. 重启所有相关服务
4. 清除浏览器 cookies 后测试

## 技术细节

### Spring Session Cookie 管理

Spring Session 默认使用 `JSESSIONID` 作为 cookie 名称。要实现动态 cookie 名称，需要：

1. 实现 `CookieSerializer` 接口
2. 根据请求上下文返回不同的 cookie 名称
3. 在读取时支持多个 cookie 名称

### 关键代码位置

- Session 配置：`sca-skeleton-auth/src/main/resources/sca-skeleton-auth-dev.yaml`
- Security 配置：`AuthSecurityConfig.java`
- 渠道隔离：`AuthorizeChannelIsolationFilter.java`
- 登录成功处理：`OAuthAuthorizeLoginSuccessHandler.java`

---

**下一步行动**：
1. 请先确认 404 错误的具体来源（哪个URL返回404）
2. 检查 system 服务和网关是否正常
3. 如果确认是 session 问题，我们再实施完整的隔离方案
