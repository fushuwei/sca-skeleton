# SCA Skeleton

## 仓库目的

`sca-skeleton` 是一个基于 **Spring Cloud Alibaba** 的微服务基础骨架（项目初始化脚手架），`sca` 即 **Spring Cloud Alibaba** 的缩写。

本项目定位为前后端分离的 Monorepo 工程模板，提供：

- **后端**：完整的微服务基础设施，包含网关、认证、系统服务等核心模块，集成 Nacos 注册/配置中心、Redis 分布式缓存、MyBatis-Plus ORM 等常用组件
- **前端**：现代化的 Monorepo 前端工程，支持多应用并行开发与共享包复用
- **工程规范**：统一的目录边界、协作约束与最佳实践，帮助团队快速启动新项目，避免重复搭建工程骨架

## 界面预览

<table align="center">
<tr>
<td align="center"><img src="https://raw.githubusercontent.com/fushuwei/sca-skeleton/refs/heads/main/docs/screenshot/01.png" width="280"></td>
<td align="center"><img src="https://raw.githubusercontent.com/fushuwei/sca-skeleton/refs/heads/main/docs/screenshot/02.png" width="280"></td>
<td align="center"><img src="https://raw.githubusercontent.com/fushuwei/sca-skeleton/refs/heads/main/docs/screenshot/03.png" width="280"></td>
</tr>
<tr>
<td align="center"><img src="https://raw.githubusercontent.com/fushuwei/sca-skeleton/refs/heads/main/docs/screenshot/04.png" width="280"></td>
<td align="center"><img src="https://raw.githubusercontent.com/fushuwei/sca-skeleton/refs/heads/main/docs/screenshot/05.png" width="280"></td>
<td align="center"><img src="https://raw.githubusercontent.com/fushuwei/sca-skeleton/refs/heads/main/docs/screenshot/06.png" width="280"></td>
</tr>
<tr>
<td align="center"><img src="https://raw.githubusercontent.com/fushuwei/sca-skeleton/refs/heads/main/docs/screenshot/07.png" width="280"></td>
<td align="center"><img src="https://raw.githubusercontent.com/fushuwei/sca-skeleton/refs/heads/main/docs/screenshot/08.png" width="280"></td>
<td align="center"><img src="https://raw.githubusercontent.com/fushuwei/sca-skeleton/refs/heads/main/docs/screenshot/09.png" width="280"></td>
</tr>
<tr>
<td align="center"><img src="https://raw.githubusercontent.com/fushuwei/sca-skeleton/refs/heads/main/docs/screenshot/10.png" width="280"></td>
<td align="center"><img src="https://raw.githubusercontent.com/fushuwei/sca-skeleton/refs/heads/main/docs/screenshot/11.png" width="280"></td>
<td align="center"><img src="https://raw.githubusercontent.com/fushuwei/sca-skeleton/refs/heads/main/docs/screenshot/12.png" width="280"></td>
</tr>
<tr>
<td align="center"><img src="https://raw.githubusercontent.com/fushuwei/sca-skeleton/refs/heads/main/docs/screenshot/13.png" width="280"></td>
<td align="center"><img src="https://raw.githubusercontent.com/fushuwei/sca-skeleton/refs/heads/main/docs/screenshot/14.png" width="280"></td>
<td align="center"><img src="https://raw.githubusercontent.com/fushuwei/sca-skeleton/refs/heads/main/docs/screenshot/15.png" width="280"></td>
</tr>
<tr>
<td align="center"><img src="https://raw.githubusercontent.com/fushuwei/sca-skeleton/refs/heads/main/docs/screenshot/16.png" width="280"></td>
<td align="center"><img src="https://raw.githubusercontent.com/fushuwei/sca-skeleton/refs/heads/main/docs/screenshot/17.png" width="280"></td>
<td align="center"><img src="https://raw.githubusercontent.com/fushuwei/sca-skeleton/refs/heads/main/docs/screenshot/18.png" width="280"></td>
</tr>
<tr>
<td align="center"><img src="https://raw.githubusercontent.com/fushuwei/sca-skeleton/refs/heads/main/docs/screenshot/19.png" width="280"></td>
<td align="center"><img src="https://raw.githubusercontent.com/fushuwei/sca-skeleton/refs/heads/main/docs/screenshot/20.png" width="280"></td>
<td align="center"><img src="https://raw.githubusercontent.com/fushuwei/sca-skeleton/refs/heads/main/docs/screenshot/21.png" width="280"></td>
</tr>
</table>

## 项目结构

```text
# omit
```

## 技术栈汇总

### 后端

| 技术栈               | 版本       | 说明                    |
|----------------------|------------|-------------------------|
| Java                 | 25         | 最新 LTS 版本           |
| Spring Boot          | 4.1.1      | 应用框架                |
| Spring Cloud         | 2025.1.3   | 微服务框架              |
| Spring Cloud Alibaba | 2025.1.0.0 | Nacos 注册/配置中心     |
| Nacos Server         | 3.2.4      | 内嵌注册/配置中心服务端 |
| MyBatis-Plus         | 3.5.17     | 增强型 MyBatis          |
| Redis (Redisson)     | 4.7.0      | 分布式锁、Session 共享  |
| MinIO SDK            | 9.0.3      | 对象存储                |
| Lombok               | 1.18.46    | 代码简化                |
| MapStruct            | 1.6.3      | 对象映射                |
| Hutool               | 5.8.47     | 工具类集合              |
| ip2region            | 3.3.7      | 离线 IP 归属地查询      |
| uuid-creator         | 6.1.1      | UUID 生成               |
| EasyCaptcha          | 1.6.2      | 验证码生成              |
| SpringDoc OpenAPI    | 3.1.1      | API 文档                |

### 前端

| 技术栈                     | 版本    | 说明                            |
|----------------------------|---------|---------------------------------|
| Vue                        | 3.5.33  | 核心框架                        |
| Vue Router                 | 5.0.6   | 路由管理                        |
| Pinia                      | 3.0.4   | 状态管理                        |
| Quasar                     | 2.21.4  | UI 框架（admin 与 portal 共用） |
| @quasar/extras             | 1.18.0  | Quasar 图标与字体资源           |
| @quasar/vite-plugin        | 1.11.0  | Quasar 的 Vite 集成插件         |
| axios                      | 1.15.2  | HTTP 请求                       |
| vue-i18n                   | 10.0.8  | 国际化（zh-CN / en-US）         |
| CodeMirror                 | 6.0.2   | 代码编辑器内核（SQL 编辑器）    |
| @codemirror/view           | 6.43.8  | 编辑器视图层                    |
| @codemirror/state          | 6.7.1   | 编辑器状态管理                  |
| @codemirror/lang-sql       | 6.10.0  | SQL 语法高亮                    |
| @codemirror/theme-one-dark | 6.1.3   | 编辑器深色主题                  |
| TipTap                     | 3.30.2  | 富文本编辑器（含 12 个扩展包）  |
| zxcvbn-ts                  | 4.1.2   | 密码强度评估                    |
| sql-formatter              | 15.8.2  | SQL 格式化                      |
| Vite                       | 8.0.16  | 构建与开发服务                  |
| @vitejs/plugin-vue         | 6.0.6   | Vue 单文件组件编译              |
| TypeScript                 | 6.0.3   | 类型系统                        |
| vue-tsc                    | 3.2.8   | 类型检查（`vue-tsc --noEmit`）  |
| sass                       | 1.99.0  | Sass 样式编译                   |
| pnpm                       | 10.33.2 | 包管理与 workspace              |
| Turborepo                  | 2.9.6   | Monorepo 任务编排               |
| ESLint                     | 10.2.1  | 代码检查                        |
| typescript-eslint          | 8.59.2  | TypeScript 检查规则             |
| eslint-plugin-vue          | 10.9.1  | Vue 单文件组件检查规则          |
| Prettier                   | 3.8.3   | 代码格式化                      |
| Vitest                     | 4.1.5   | 单元测试                        |
| @vue/test-utils            | 2.4.9   | Vue 组件测试工具                |
| Playwright                 | 1.61.0  | 端到端测试                      |

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
