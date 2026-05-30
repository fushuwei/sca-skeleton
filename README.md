# SCA Skeleton

## 仓库目的

`sca-skeleton` 是一个前后端分离的 Monorepo 初始化骨架，用于统一仓库治理、目录边界与工程协作方式。

当前阶段目标：

- 提供可持续扩展的基础结构
- 固化仓库级规则与初始化基线
- 为后续前后端业务开发提供统一入口

## 项目结构

```text
sca-skeleton/
├── .cursor/rules/                 # 根仓库规则
├── .editorconfig                  # 全仓编辑器规范
├── .gitignore                     # 全仓忽略策略
├── README.md                      # 仓库入口文档
├── package.json                   # 根前端命令入口（pnpm --dir 转发）
├── pom.xml                        # 根聚合 Maven POM
├── sca-skeleton-backend/          # 后端工程（Maven 多模块）
│   ├── .cursor/rules/
│   ├── README.md
│   ├── lombok.config
│   ├── pom.xml
│   ├── sca-skeleton-dependencies/ # 统一依赖 BOM
│   ├── sca-skeleton-gateway/      # API 网关服务
│   ├── sca-skeleton-auth/         # 认证服务（OAuth2 Authorization Server）
│   ├── sca-skeleton-system/       # 系统领域服务
│   ├── sca-skeleton-starters/     # 可复用 Starter 模块
│   │   ├── sca-skeleton-starter-core        # 核心基础（异常体系、工具类、TraceId）
│   │   ├── sca-skeleton-starter-web         # Web 通用（统一响应体、限流注解）
│   │   ├── sca-skeleton-starter-logging     # 日志增强（操作日志 AOP）
│   │   ├── sca-skeleton-starter-mybatis-plus # MyBatis-Plus 增强
│   │   ├── sca-skeleton-starter-feign       # OpenFeign 增强
│   │   ├── sca-skeleton-starter-captcha      # 验证码生成与校验
│   │   └── sca-skeleton-starter-security     # OAuth2 Redis 存储、资源鉴权
│   └── sca-skeleton-middlewares/  # 中间件模块
├── sca-skeleton-frontend/         # 前端工程（Turborepo Monorepo）
│   ├── apps/
│   │   ├── admin/                 # 后台管理系统（Vue3 + Vite + Quasar）
│   │   └── portal/                # 前台门户（Vue3 + Vite + ant-design-vue + unocss）
│   ├── packages/
│   │   ├── config/                # 共享工程配置（TS/ESLint/Prettier）
│   │   ├── shared/                # 共享常量与通用逻辑（OAuth、PKCE）
│   │   └── ui/                    # 共享 UI 组件
│   ├── package.json
│   ├── turbo.json
│   └── pnpm-workspace.yaml
├── deploy/                        # 部署与运维资产
└── scripts/                       # 仓库级脚本
```

## 技术栈汇总

### 后端

| 类别 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 语言 | Java | 25 | 最新 LTS 版本 |
| 框架 | Spring Boot | 4.0.6 | 应用框架 |
| 云原生 | Spring Cloud | 2025.1.1 | 微服务框架 |
| | Spring Cloud Alibaba | 2025.1.0.0 | Nacos 注册/配置中心 |
| ORM | MyBatis-Plus | 3.5.16 | 增强型 MyBatis |
| 缓存 | Redis (Redisson) | 4.3.0 | 分布式锁、Session 共享 |
| 工具 | Lombok | 1.18.46 | 代码简化 |
| | MapStruct | 1.6.3 | 对象映射 |
| 工具库 | Hutool | 5.8.44 | 工具类集合 |
| 文档 | SpringDoc OpenAPI | 3.0.3 | API 文档 |

### 前端

| 类别 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 框架 | Vue | 3.5.33 | 核心框架 |
| 构建 | Vite | 8.0.10 | 构建与开发服务 |
| 类型 | TypeScript | 6.0.3 | 类型系统 |
| 状态 | Pinia | 3.0.4 | 状态管理 |
| 路由 | Vue Router | 5.0.6 | 路由管理 |
| 包管理 | pnpm | 10.33.2 | 包管理与 workspace |
| 编排 | Turborepo | 2.9.6 | Monorepo 任务编排 |
| Admin UI | Quasar | 2.19.3 | 后台管理 UI 框架 |
| Portal UI | ant-design-vue | 4.2.6 | 前台门户 UI 组件库 |
| | unocss | 66.6.8 | 原子化 CSS |
| HTTP | axios | 1.15.2 | HTTP 请求 |
| 国际化 | vue-i18n | ^10.0.7 | 国际化支持 |

## 架构特点

- **微服务架构**：网关、认证、系统服务物理隔离，通过 Nacos 服务发现通信
- **OAuth2 + PKCE**：安全的认证授权流程，支持 Authorization Code + PKCE 模式
- **速率限流**：基于 Redis 的网关层限流保护，防暴力尝试
- **共享包复用**：前端 Monorepo 结构，`@repo/shared` 和 `@repo/ui` 实现代码复用
- **配置分离**：development/test/production 环境配置分离
- **Starter 模式**：后端抽象常用能力为可复用 Starter 模块

## 开发环境启动

### 前端启动（推荐从仓库根目录执行）

```bash
# 安装依赖
pnpm install

# 启动全部应用（并行）
pnpm dev

# 启动后台管理
pnpm dev:admin

# 启动前台门户
pnpm dev:portal
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
