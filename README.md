# SCA Skeleton

## 仓库目的

`sca-skeleton` 是一个前后端分离的 Monorepo 初始化骨架，用于统一仓库治理、目录边界与工程协作方式。

## 界面预览

<table align="center">
<tr>
<td align="center"><a href="docs/screenshot/01.png" target="_blank"><img src="docs/screenshot/01.png" width="280"></a></td>
<td align="center"><a href="docs/screenshot/02.png" target="_blank"><img src="docs/screenshot/02.png" width="280"></a></td>
<td align="center"><a href="docs/screenshot/03.png" target="_blank"><img src="docs/screenshot/03.png" width="280"></a></td>
</tr>
<tr>
<td align="center"><a href="docs/screenshot/04.png" target="_blank"><img src="docs/screenshot/04.png" width="280"></a></td>
<td align="center"><a href="docs/screenshot/05.png" target="_blank"><img src="docs/screenshot/05.png" width="280"></a></td>
<td align="center"><a href="docs/screenshot/06.png" target="_blank"><img src="docs/screenshot/06.png" width="280"></a></td>
</tr>
<tr>
<td align="center"><a href="docs/screenshot/07.png" target="_blank"><img src="docs/screenshot/07.png" width="280"></a></td>
<td align="center"><a href="docs/screenshot/08.png" target="_blank"><img src="docs/screenshot/08.png" width="280"></a></td>
<td align="center"><a href="docs/screenshot/09.png" target="_blank"><img src="docs/screenshot/09.png" width="280"></a></td>
</tr>
<tr>
<td align="center"><a href="docs/screenshot/10.png" target="_blank"><img src="docs/screenshot/10.png" width="280"></a></td>
<td align="center"><a href="docs/screenshot/11.png" target="_blank"><img src="docs/screenshot/11.png" width="280"></a></td>
<td align="center"><a href="docs/screenshot/12.png" target="_blank"><img src="docs/screenshot/12.png" width="280"></a></td>
</tr>
<tr>
<td align="center"><a href="docs/screenshot/13.png" target="_blank"><img src="docs/screenshot/13.png" width="280"></a></td>
<td align="center"><a href="docs/screenshot/14.png" target="_blank"><img src="docs/screenshot/14.png" width="280"></a></td>
<td align="center"><a href="docs/screenshot/15.png" target="_blank"><img src="docs/screenshot/15.png" width="280"></a></td>
</tr>
<tr>
<td align="center"><a href="docs/screenshot/16.png" target="_blank"><img src="docs/screenshot/16.png" width="280"></a></td>
<td align="center"><a href="docs/screenshot/17.png" target="_blank"><img src="docs/screenshot/17.png" width="280"></a></td>
<td align="center"><a href="docs/screenshot/18.png" target="_blank"><img src="docs/screenshot/18.png" width="280"></a></td>
</tr>
</table>

## 项目结构

```text
# omit
```

## 技术栈汇总

### 后端

| 类别   | 技术                 | 版本       | 说明                   |
|--------|----------------------|------------|------------------------|
| 语言   | Java                 | 25         | 最新 LTS 版本          |
| 框架   | Spring Boot          | 4.0.6      | 应用框架               |
| 云原生 | Spring Cloud         | 2025.1.1   | 微服务框架             |
|        | Spring Cloud Alibaba | 2025.1.0.0 | Nacos 注册/配置中心    |
| ORM    | MyBatis-Plus         | 3.5.16     | 增强型 MyBatis         |
| 缓存   | Redis (Redisson)     | 4.3.0      | 分布式锁、Session 共享 |
| 工具   | Lombok               | 1.18.46    | 代码简化               |
|        | MapStruct            | 1.6.3      | 对象映射               |
| 工具库 | Hutool               | 5.8.44     | 工具类集合             |
| 文档   | SpringDoc OpenAPI    | 3.0.3      | API 文档               |

### 前端

| 类别      | 技术           | 版本    | 说明               |
|-----------|----------------|---------|--------------------|
| 框架      | Vue            | 3.5.33  | 核心框架           |
| 构建      | Vite           | 8.0.16  | 构建与开发服务     |
| 类型      | TypeScript     | 6.0.3   | 类型系统           |
| 状态      | Pinia          | 3.0.4   | 状态管理           |
| 路由      | Vue Router     | 5.0.6   | 路由管理           |
| 包管理    | pnpm           | 10.33.2 | 包管理与 workspace |
| 编排      | Turborepo      | 2.9.6   | Monorepo 任务编排  |
| Admin UI  | Quasar         | 2.19.3  | 后台管理 UI 框架   |
| Portal UI | ant-design-vue | 4.2.6   | 前台门户 UI 组件库 |
|           | unocss         | 66.6.8  | 原子化 CSS         |
| HTTP      | axios          | 1.15.2  | HTTP 请求          |
| 国际化    | vue-i18n       | ^10.0.7 | 国际化支持         |

## 架构特点

- **微服务架构**：网关、认证、系统服务物理隔离，通过 Nacos 服务发现通信
- **速率限流**：基于 Redis 的网关层限流保护，防暴力尝试
- **共享包复用**：前端 Monorepo 结构，`@repo/shared` 和 `@repo/ui` 实现代码复用
- **配置分离**：development/production 环境配置分离
- **Starter 模式**：后端抽象常用能力为可复用 Starter 模块

## 开发环境启动

### 前端启动（推荐从仓库根目录执行）

```bash
# 安装依赖
pnpm install

# 启动全部应用（并行）
pnpm dev

# 启动后台管理
nohup pnpm dev:admin > dev.log 2>&1 &

# 启动前台门户
nohup pnpm dev:portal > dev.log 2>&1 &
```

### 后端启动

```bash
cd sca-skeleton-backend

# 启动网关服务
mvn spring-boot:run -pl sca-skeleton-gateway

# 启动认证服务
mvn spring-boot:run -pl sca-skeleton-auth
```

## 协作约束（摘要）

- 根目录仅承载治理与聚合能力，不承载业务实现
- 前后端目录物理隔离，边界不可跨越
- 子工程规则目录固定：
    - `sca-skeleton-backend/.cursor/rules/`
    - `sca-skeleton-frontend/.cursor/rules/`
