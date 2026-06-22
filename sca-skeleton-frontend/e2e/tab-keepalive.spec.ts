import { test, expect } from "@playwright/test";

/**
 * 验证 Tab 关闭后重新打开时组件被重建、接口重新请求。
 *
 * 前提：需要后端服务与前端开发服务器同时运行。
 * 认证：使用已保存的浏览器状态（先手动登录一次并保存 storageState）。
 *
 * 运行方式：
 *   1. pnpm --filter @sca/admin dev        # 启动前端
 *   2. npx playwright test                  # 运行测试
 */

test.describe("Tab Keep-Alive", () => {
  test("关闭用户管理 Tab 后重新打开应重新请求后端接口", async ({ page }) => {
    // 监听所有 /api/sys/user/page 请求
    const userPageRequests: string[] = [];
    page.on("request", (req) => {
      if (req.url().includes("/api/sys/user/page")) {
        userPageRequests.push(req.url());
      }
    });

    // 导航到工作台
    await page.goto("/dashboard");
    await page.waitForLoadState("networkidle");

    // 点击侧栏菜单中的「用户管理」
    await page.click('[data-testid="menu-SystemUser"], a[href="/system/user"]');
    await page.waitForLoadState("networkidle");

    // 断言：首次打开触发了用户分页请求
    expect(userPageRequests.length).toBeGreaterThanOrEqual(1);

    // 关闭用户管理 Tab（点击关闭按钮）
    const closeBtn = page.locator(".main-tab-close-btn").last();
    if (await closeBtn.isVisible()) {
      await closeBtn.click();
      await page.waitForTimeout(300);
    }

    // 记录当前请求数
    const countAfterClose = userPageRequests.length;

    // 重新打开用户管理
    await page.click('[data-testid="menu-SystemUser"], a[href="/system/user"]');
    await page.waitForLoadState("networkidle");

    // 断言：重新打开触发了新的请求（证明组件被重建而非复用缓存）
    expect(userPageRequests.length).toBeGreaterThan(countAfterClose);
  });
});
