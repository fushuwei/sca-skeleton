# SCA Skeleton

## 仓库说明

本仓库为前后端 Monorepo 骨架，仅包含仓库级治理资产与初始化结构，不包含业务实现代码。

## 仓库结构

- `sca-skeleton-backend/`：后端工程根目录（Maven 模块）
- `sca-skeleton-frontend/`：前端工程根目录（当前阶段按普通目录管理）
- `deploy/`：部署与运维资产目录
- `scripts/`：仓库级脚本目录

## 初始化方式

- 根目录使用聚合 `pom.xml` 管理 Maven 构建入口。
- 根目录 `package.json` 通过 `pnpm --dir ./sca-skeleton-frontend` 转发前端命令。

## 协作约束

- 根目录不承载业务功能代码。
- 前后端保持物理隔离与职责隔离。
- 子工程规则目录固定为：
  - `sca-skeleton-backend/.cursor/rules/`
  - `sca-skeleton-frontend/.cursor/rules/`
