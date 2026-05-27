# 数据中台前端 Monorepo

## 目录结构和说明

```text
sca-skeleton-frontend/
├─ apps/
│  ├─ admin/            # 后台管理系统（Vue3 + Vite + TS）
│  └─ portal/           # 前台门户（Vue3 + Vite + TS）
├─ packages/
│  ├─ config/           # 共享工程配置（TS/ESLint/Prettier）
│  ├─ shared/           # 共享常量与通用逻辑
│  └─ ui/               # 共享 UI 组件
├─ package.json         # Monorepo 根脚本（dev/build/lint/test）
├─ pnpm-workspace.yaml  # pnpm workspace 定义
└─ turbo.json           # Turborepo pipeline 配置
```

## 技术栈和版本以及用途

- `vue@3.5.33`：前端核心框架
- `vite@8.0.10`：构建与本地开发服务
- `typescript@6.0.3`：类型系统
- `turbo@2.9.6`：Monorepo 任务编排与缓存
- `pnpm@10.33.2`：包管理与 workspace 管理
- `@vitejs/plugin-vue@6.0.6`：Vite 的 Vue 支持
- `vue-router@5.0.6`：路由管理
- `pinia@3.0.4`：状态管理
- `ant-design-vue@4.2.6`：主 UI 组件库
- `axios@1.15.2`：HTTP 请求
- `unocss@66.6.8`：原子化 CSS
- `eslint@10.2.1`：代码规范检查
- `prettier@3.8.3`：代码格式化
- `vitest@4.1.5`：单元测试运行器
- `@vue/test-utils@2.4.9`：Vue 组件测试工具

## 开发环境网关代理

本地 `pnpm dev` 时，`/api-dev` 由 Vite 代理到 `VITE_DEV_PROXY_TARGET`（默认 `http://localhost:9999`）。

联调远程网关时，在各 app 目录复制 `.env.development.local.example` 为 `.env.development.local`，仅改网关地址，例如：

```bash
# apps/admin 或 apps/portal
cp .env.development.local.example .env.development.local
# 编辑 VITE_DEV_PROXY_TARGET=http://<远程网关 IP>:9999
```

OAuth 与 API 均经 `http://localhost:5173|5174/api-dev` 走同一代理，无需单独改 OAuth 地址。

## 启动命令

- 安装依赖：`pnpm install`
- 启动全部应用（并行）：`pnpm dev`
- 启动后台管理：`pnpm dev:admin`
- 启动前台门户：`pnpm dev:portal`

### 环境变量说明

| 变量 | development | test / production |
|------|-------------|-------------------|
| `VITE_DEV_PROXY_TARGET` | 有。仅 `pnpm dev` 时 Vite 把 `/api-dev` 转发到该网关地址 | 无。构建产物不走 Vite，由 Nginx 将 `/api`、`/api-test` 反代到网关 |
| `VITE_API_BASE_URL` | `/api-dev` | `/api-test` 或 `/api` |
| OAuth 授权/换票地址 | 由 `VITE_API_BASE_URL` + 当前站点 origin 自动拼接 | 同上（需 Nginx 与 API 同源路径） |

联调远程网关时，只改 `apps/<app>/.env.development` 中的 `VITE_DEV_PROXY_TARGET` 即可。
- 项目构建：`pnpm build`
- 代码检查：`pnpm lint`
- 单元测试：`pnpm test`
