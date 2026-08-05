import type { Router } from "vue-router";
import { refreshAccessToken, isNotificationHandled } from "@repo/shared";
import { getAdminOAuthConfig } from "../config/oauth";
import { WHITE_LIST_ROUTE_NAMES } from "./routes";
import { useAuthStore } from "../stores/auth";
import { collectLeafMenuPaths } from "../utils/menu-tree";
import {
  MENUS_STORAGE_KEY,
  REFRESH_TOKEN_STORAGE_KEY,
  TOKEN_STORAGE_KEY
} from "../constants/auth-storage";

/**
 * 使用已保存的 refresh_token 静默续期 access_token。
 * 成功时更新 localStorage 与 Pinia store，失败时清除全部令牌与菜单缓存。
 *
 * @returns true 表示续期成功，调用方可继续导航
 */
async function trySilentRefresh(): Promise<boolean> {
  const refreshToken = localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY);
  if (!refreshToken) {
    return false;
  }
  try {
    const oauthConfig = getAdminOAuthConfig();
    const tokenResponse = await refreshAccessToken(oauthConfig, refreshToken);
    localStorage.setItem(TOKEN_STORAGE_KEY, tokenResponse.access_token);
    if (tokenResponse.refresh_token) {
      localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, tokenResponse.refresh_token);
    }
    const authStore = useAuthStore();
    authStore.syncOAuthTokens(tokenResponse.access_token, tokenResponse.refresh_token);
    return true;
  } catch (error) {
    // 网络异常或浏览器中止请求（如 Firefox 快速刷新 abort fetch）：
    // 非真正的 refresh_token 失效，保留令牌让下次刷新重试
    if (error instanceof TypeError) {
      return false;
    }
    // 后端明确拒绝（refresh_token 已失效）：清除令牌
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
    localStorage.removeItem(MENUS_STORAGE_KEY);
    const authStore = useAuthStore();
    authStore.token = "";
    authStore.refreshToken = "";
    authStore.menus = [];
    authStore.dynamicReady = false;
    return false;
  }
}

export function setupRouterGuards(router: Router): void {
  router.beforeEach(async (to, _from) => {
    const authStore = useAuthStore();
    const routeName = String(to.name ?? "");
    const isWhiteRoute = WHITE_LIST_ROUTE_NAMES.has(routeName);
    const leafPaths = collectLeafMenuPaths(authStore.menus);
    const mayBeDynamicPath = leafPaths.includes(to.path);

    // ── 未登录：先尝试静默 refresh_token 续期 ──
    if (!authStore.isLoggedIn && !isWhiteRoute) {
      const refreshed = await trySilentRefresh();
      if (!refreshed) {
        // 续期失败：重定向到登录页，携带回跳地址
        return { name: "Login", query: { redirect: to.fullPath } };
      }
      // 静默续期成功，继续执行下方的"已登录"逻辑
    }

    // ── 已登录但访问登录页：重定向到首页 ──
    if (authStore.isLoggedIn && routeName === "Login") {
      return { name: "Root" };
    }

    // ── 已登录 ──
    if (authStore.isLoggedIn) {
      authStore.ensureRoutes(router);
      // profile 为空或缺少 isSuperadmin 字段（兼容旧版 profile 缓存）时重新拉取
      if (!authStore.profile || authStore.profile.isSuperadmin === undefined) {
        try {
          await authStore.fetchProfile();
        } catch (error) {
          // 网络异常或浏览器中止请求（如 Firefox 快速刷新 abort fetch）：
          // 非认证失败，保留令牌避免误登出，中止当前导航
          if (isNotificationHandled(error)) {
            return false;
          }
          // 真正的认证失败（401 且 refresh 也失败）：清除令牌，重定向到登录页
          authStore.token = "";
          authStore.refreshToken = "";
          authStore.profile = null;
          authStore.dynamicReady = false;
          localStorage.removeItem(TOKEN_STORAGE_KEY);
          localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
          localStorage.removeItem(MENUS_STORAGE_KEY);
          return { name: "Login", query: { redirect: to.fullPath } };
        }
      }
      if (routeName === "NotFound" && mayBeDynamicPath) {
        return { path: to.fullPath, replace: true };
      }
    }
  });
}
