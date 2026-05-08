# sca-skeleton-backend

后端工程聚合根目录，仅承载模块聚合与构建治理。

## 模块结构

- `sca-skeleton-dependencies`：统一依赖与版本治理（BOM）。
- `sca-skeleton-gateway`：网关服务骨架模块。
- `sca-skeleton-auth`：认证服务骨架模块。
- `sca-skeleton-system`：系统领域服务骨架模块。
- `sca-skeleton-starters`：可复用 starter 聚合模块。
  - `sca-skeleton-starter-core`
  - `sca-skeleton-starter-web`
  - `sca-skeleton-starter-logging`
  - `sca-skeleton-starter-captcha`
