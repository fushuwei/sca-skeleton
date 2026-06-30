# Instructions

## Playwright MCP 使用规范

调用 Playwright 浏览器工具打开 Chrome 浏览器后，不要主动关闭浏览器，保持浏览器打开状态以便后续操作和重复使用当前打开的浏览器。

## Playwright MCP 截图规范

使用 Playwright 浏览器工具时，所有截图必须保存到 `.playwright-mcp/` 目录下，禁止保存到项目根目录。

示例：

```
filename: ".playwright-mcp/screenshot-name.png"
```

## 语言规范

始终使用中文与用户对话。
