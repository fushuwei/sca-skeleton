# Close Report — 999-example-feature

> 由 OpenCode 编写。验证修复并确认是否闭环。

**验证日期：** YYYY-MM-DD  
**对应 QA：** qa-report.md（YYYY-MM-DD）  
**对应 Dev Response：** dev-response.md（YYYY-MM-DD）

## 逐条验证

| ID | QA 结论 | Cursor 回应 | 裁决 | 结果 |
|----|---------|-------------|------|------|
| P0-01 | …… | fixed | 验证通过 | pass |
| P1-01 | …… | disputed | 对照 spec，QA 不成立 | pass |
| P2-01 | …… | need-clarification | 已补充复现，确认为 bug | fail |

**结果说明：** `pass` | `fail` | `spec-update-required`

## 验收清单复核（可选）

| 验收 ID | 结果 | 说明 |
|---------|------|------|
| A1 | pass / fail | |
| A2 | pass / fail | |

## 最终结论

- [ ] **CLOSED** — 全部通过，feature 闭环
- [ ] **REOPEN** — 仍有问题，退回 Cursor 继续修复

**遗留风险（如有）：** ……
