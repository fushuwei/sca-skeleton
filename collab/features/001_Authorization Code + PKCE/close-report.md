# Close Report — OAuth 2.1 Authorization Code + PKCE

> **闭环日期**: 2026-05-28

## 整改验证结果

| # | 级别 | 问题 | 状态 | 验证方式 |
|---|------|------|------|----------|
| P0-1 | P0 | Token 吊销端点未放行网关白名单 | ✅ 已修复 | `gateway-dev.yaml` 新增 `/auth/oauth2/revoke` |
| P1-1 | P1 | 验证码未接入认证流程 | ⏸ 暂缓 | 按你的要求本次不处理 |
| P1-2 | P1 | 无 DPoP/mTLS Token 绑定 | ⏸ 暂缓 | 架构级改造，建议单独开需求 |
| P1-3 | P1 | `/oauth2/authorize` 无限流 | ✅ 已修复 | 新增独立路由 + RateLimiter (20/40) |
| P2-1 | P2 | Redis `set+expire` 非原子 | ✅ 已修复 | 改为 `set(key, value, ttl, unit)` 原子写入 |
| P2-2 | P2 | Gateway CORS 生产过松 | ⏸ 暂缓 | 缺少 prod 配置基线，后续收敛 |
| P2-3 | P2 | `PasswordEncoder` 含 `{noop}` | ✅ 已修复 | 改为 `PasswordEncoderFactories.createDelegatingPasswordEncoder()` |
| P2-4 | P2 | 授权码 TTL 未显式配置 | ✅ 已修复 | 显式设置 `authorizationCodeTimeToLive=60s` |
| P2-5 | P2 | Auth 内 JWT ResourceServer 歧义 | ✅ 已修复 | 移除 `.oauth2ResourceServer(jwt)` 伪配置 |
| P2-6 | P2 | `LoginChannelContext` 默认回退 ADMIN | ✅ 已修复 | 回退时显式失败 |
| P3-1 | P3 | client_id 脏索引未清理 | ✅ 已修复 | ID 缓存 miss 时主动删除 client_id 索引 |
| P3-2 | P3 | `SecurityUtils` JWT 分支 | ⏸ 暂缓 | 保留兼容，后续按需清理 |
| P3-3 | P3 | Session cookie domain/path | ⏸ 暂缓 | 结合生产域名策略统一配置 |
| P3-4 | P3 | 账户状态语义重叠 | ⏸ 暂缓 | 需产品/安全策略确认 |
| P3-5 | P3 | 异常信息文案 | ⏸ 暂缓 | 低于核心协议安全项优先级 |

## 结论

**本次可闭环。**

已落地整改 8/15 项（含全部 P0 和 3/4 的 P2），核心安全隐患已收敛。剩余项已明确暂缓理由和后续计划。

### 关于 Logout 设计的补充说明

针对你提出的"cursor 弃用 SAS 内置 `/oauth2/revoke` 自写 logout"的疑问，实际情况是：

- **Token 吊销** — 前端 `revokeOAuthToken()` 调用的正是 SAS 内置的 `/oauth2/revoke` (RFC 7009)，没有绕过。
- **Session 清理** — `AuthSessionController` 的 `/session/logout` 做的是 `session.invalidate()` + `SecurityContextHolder.clearContext()`，这是 **SAS 内置 `/oauth2/revoke` 不做的事情**。

两步骤协作的原因：如果不销毁 Auth 服务的 JSESSIONID，logout 后跳回 `/oauth2/authorize` 时 Spring Security 检测到 Session 仍有效，会**静默签发新授权码**，用户连密码都不用输，违背了退出登录的语义。

这个设计本身合理，改进空间仅在于：可以将 `/session/logout` 替换为 Spring Security 内置的 `POST /logout` 以减少手写代码量，但功能等价。
