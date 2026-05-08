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
├── pom.xml                        # 根聚合 Maven POM
├── package.json                   # 根前端命令入口（pnpm --dir 转发）
├── sca-skeleton-backend/          # 后端工程
│   ├── .cursor/rules/
│   ├── README.md
│   ├── lombok.config
│   └── pom.xml
├── sca-skeleton-frontend/         # 前端工程（当前按普通目录管理）
│   ├── .cursor/rules/
│   ├── README.md
│   ├── package.json
│   ├── turbo.json
│   ├── pnpm-workspace.yaml
│   ├── apps/
│   └── packages/
├── deploy/                        # 部署与运维资产
└── scripts/                       # 仓库级脚本
```

## 技术栈汇总

### 后端

- 构建与模块管理：Maven（根聚合 + 后端子工程）
- 代码工具：Lombok（通过 `sca-skeleton-backend/lombok.config` 统一约束）
- 语言与框架：待后端工程规则进一步明确

### 前端

- 包管理与命令执行：pnpm
- Monorepo 编排：Turborepo（`turbo.json`）
- 工程组织：`apps/*` + `packages/*`
- 框架与 UI 技术栈：待前端工程规则进一步明确

## 启动方式

### 前端启动（推荐从仓库根目录执行）

```bash
pnpm dev
```

等价于：

```bash
pnpm --dir ./sca-skeleton-frontend dev
```

### 后端启动（当前阶段）

后端已完成 Maven 骨架初始化，具体运行命令待后端子工程引入实际应用模块后补充。

## 协作约束（摘要）

- 根目录仅承载治理与聚合能力，不承载业务实现
- 前后端目录物理隔离，边界不可跨越
- 子工程规则目录固定：
  - `sca-skeleton-backend/.cursor/rules/`
  - `sca-skeleton-frontend/.cursor/rules/`
