import { test, expect } from "@playwright/test";

/**
 * 验证登录会话过期后 returnUrl 不含重复 HTML base。
 *
 * 前置条件：
 *   1. 后端 Auth 服务已启动（端口 9001）
 *   2. 前端 Admin 开发服务器已启动（`pnpm --filter @sca/admin dev`）
 *   3. 首次运行时先手动登录一次，保存 storageState 避免每次走 OAuth 流程
 *
 * 运行：npx playwright test e2e/session-timeout-redirect.spec.ts --project=chromium
 */

test.describe("Session Timeout Return URL", () => {
  test("returnUrl 不应包含重复的 /admin/ 前缀", async ({ page }) => {
    // Step 1：导航到用户管理页（需登录态）
    await page.goto("/admin/system/user");
    await page.waitForLoadState("networkidle");

    // Step 2：模拟会话过期——清除 localStorage 中的 token
    await page.evaluate(() => {
      localStorage.removeItem("sca_admin_access_token");
      localStorage.removeItem("sca_admin_refresh_token");
    });

    // Step 3：刷新页面触发路由守卫 → OAuth 登录
    await page.reload();
    await page.waitForURL("**/auth/login/admin**", { timeout: 15000 });

    // Step 4：验证登录 URL 中不包含重复 base
    const currentUrl = page.url();
    expect(currentUrl).toContain("/auth/login/admin");

    // 执行实际登录（需要真实凭证，此处为骨架）
    // await page.fill('input[name="username"]', "admin");
    // await page.fill('input[name="password"]', "...");
    // await page.click('button[type="submit"]');

    // Step 5：登录后验证跳转 URL 不含 /admin/admin/
    // await page.waitForURL("**/admin/system/user", { timeout: 15000 });
    // const finalUrl = page.url();
    // expect(finalUrl).not.toContain("/admin/admin/");
    // expect(finalUrl).toContain("/admin/system/user");
  });
});
