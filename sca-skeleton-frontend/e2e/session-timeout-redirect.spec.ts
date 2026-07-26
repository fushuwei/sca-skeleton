import { test, expect } from "@playwright/test";

/**
 * 验证登录会话过期后重定向到 SPA 登录页。
 *
 * 密码模式下，令牌失效后路由守卫重定向到 SPA 内的 /login 页面，
 * 不再跳转到后端 Thymeleaf 登录页。
 *
 * 前置条件：
 *   1. 后端服务已启动（Gateway + Auth + System）
 *   2. 前端 Admin 开发服务器已启动（`pnpm --filter @sca/admin dev`）
 *
 * 运行：npx playwright test e2e/session-timeout-redirect.spec.ts --project=chromium
 */

test.describe("Session Timeout Redirect", () => {
  test("令牌失效后重定向到 SPA 登录页", async ({ page }) => {
    // Step 1：导航到用户管理页（需登录态）
    await page.goto("/admin/system/user");
    await page.waitForLoadState("networkidle");

    // Step 2：模拟会话过期——清除 localStorage 中的 token
    await page.evaluate(() => {
      localStorage.removeItem("admin_access_token");
      localStorage.removeItem("admin_refresh_token");
    });

    // Step 3：刷新页面触发路由守卫 → 重定向到 SPA 登录页
    await page.reload();
    await page.waitForURL("**/admin/login**", { timeout: 15000 });

    // Step 4：验证登录 URL 包含 redirect 回跳参数
    const currentUrl = page.url();
    expect(currentUrl).toContain("/admin/login");
    expect(currentUrl).toContain("redirect=");
  });
});
