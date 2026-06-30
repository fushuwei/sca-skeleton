# Instructions

## Playwright MCP 使用规范

调用 Playwright 浏览器工具打开 Chrome 浏览器后，不要主动关闭浏览器，保持浏览器打开状态以便后续操作和重复使用当前打开的浏览器。

## Playwright MCP 截图规范

使用 Playwright 浏览器工具时，所有截图必须保存到 `.playwright-mcp/` 目录下，禁止保存到项目根目录。

示例：

```
filename: ".playwright-mcp/screenshot-name.png"
```

## 前端效果修改规范

修改任何前端 UI 效果后，必须使用 Playwright MCP 工具进行自动测试验证，禁止仅凭代码推断效果。测试流程：
1. 修改代码后，使用 Playwright 打开对应页面
2. 截图对比修改前后的效果
3. 模拟用户操作（点击、输入等）验证交互行为
4. 确认无误后再告知用户

## 语言规范

始终使用中文与用户对话。
