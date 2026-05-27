# collab

人与 AI 协作交付工作流目录。

| 角色 | 工具 | 职责 |
|------|------|------|
| 需求 | 人 | 编写需求与验收标准 |
| 开发 | Cursor | 实现功能、回应 QA |
| 审查 | OpenCode | QA 检查、验证修复、闭环确认 |

本目录**不是**业务代码。实际实现位于 `sca-skeleton-backend/` 与 `sca-skeleton-frontend/`。

## 目录结构

```text
collab/
├── README.md
└── features/
    └── {编号}-{feature-slug}/
        ├── spec.md
        ├── qa-report.md
        ├── dev-response.md
        └── close-report.md
```

## 命名规范

| 对象 | 规则 | 示例 |
|------|------|------|
| Feature 目录 | `{编号}-{feature-slug}/` | `001-oauth2-pkce` |
| Slug | 小写 kebab-case | `admin-user-crud` |
| 问题编号 | `{级别}-{序号}` | `P0-01`、`P1-03` |
| 验收编号 | `A{序号}` | `A1`、`A2` |

严重级别：`P0`（严重）、`P1`（重要）、`P2`（一般/建议）。

## 4 个 md 文件是否需要加编号？

**不需要。** 理由：

- 每个 feature 目录下只有 4 个文件，名称已表达职责，不会混淆
- 工作顺序由流程决定，而非文件名排序
- 不加编号更便于引用（如「见 `qa-report.md`」）

若你希望在文件管理器中按阶段排序，可选用前缀（非必须）：

```text
01-spec.md
02-qa-report.md
03-dev-response.md
04-close-report.md
```

默认约定：**不加编号**，保持 `spec.md` 等短名。

## 工作流

```text
spec.md
   ↓
Cursor 开发（代码在 backend/frontend）
   ↓
OpenCode → qa-report.md
   ↓
Cursor → dev-response.md + 代码修复
   ↓
OpenCode → close-report.md
   ├─ CLOSED → 闭环
   └─ REOPEN  → 更新 qa-report.md → 继续循环
```

## 文件归属

| 文件 | 归属 | Cursor | OpenCode | 人 |
|------|------|--------|----------|-----|
| `spec.md` | 人 | 只读 | 只读 | 可写 |
| `qa-report.md` | OpenCode | 只读 | 可写 | 只读 |
| `dev-response.md` | Cursor | 可写 | 只读 | 只读 |
| `close-report.md` | OpenCode | 只读 | 可写 | 只读 |

**规则：**

- Cursor 不得修改 `qa-report.md`、`close-report.md`
- OpenCode 不得修改 `dev-response.md`
- 对 QA 的异议写在 `dev-response.md`；OpenCode 在 `close-report.md` 中裁决
- 只有 OpenCode 能在 `close-report.md` 中将 feature 标记为 `CLOSED`

## 多轮 QA

当 `close-report.md` 结论为 `REOPEN` 时：

1. OpenCode 更新 `qa-report.md`（仅保留未关闭项，或标注新一轮）
2. Cursor 更新 `dev-response.md` 并修复代码
3. OpenCode 重写 `close-report.md`

默认覆盖当前文件，历史版本靠 Git 追溯。重要节点可选手动快照：`qa-report.YYYYMMDD.md`。

## 文件格式

### spec.md

- 功能描述
- 范围 / 不做的事（可选）
- 验收清单（`A1`、`A2`…）

### qa-report.md

- 仅列问题，不写修复状态
- 每条：编号、级别、描述、影响文件

### dev-response.md

对 `qa-report.md` 逐条回应：

| 状态 | 含义 |
|------|------|
| `fixed` | 已修复，待验证 |
| `disputed` | 不认同 QA，需说明依据 |
| `cannot-fix` | 认同是问题，当前无法修复 |
| `need-clarification` | 缺少复现步骤、环境等 |

### close-report.md

- 逐条验证 `qa-report.md` 中的问题
- 裁决 `dev-response.md` 中的 `disputed` / `need-clarification`
- 每条结果：`pass` | `fail` | `spec-update-required`
- 最终结论：`CLOSED` | `REOPEN`

## 新建 Feature

```bash
mkdir -p collab/features/001-my-feature
touch collab/features/001-my-feature/spec.md
```

可参考示例目录：`collab/features/999-example-feature/`。
