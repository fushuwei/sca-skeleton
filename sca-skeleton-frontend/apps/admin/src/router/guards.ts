import type { Router } from "vue-router";
import { refreshAccessToken, startOAuthLogin } from "@repo/shared";
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
    // 持久化新令牌
    localStorage.setItem(TOKEN_STORAGE_KEY, tokenResponse.access_token);
    if (tokenResponse.refresh_token) {
      localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, tokenResponse.refresh_token);
    }
    // 同步 Pinia 内存态，确保 isLoggedIn 立即变为 true
    const authStore = useAuthStore();
    authStore.syncOAuthTokens(tokenResponse.access_token, tokenResponse.refresh_token);
    return true;
  } catch {
    // 续期失败：清除全部令牌与菜单，后续将由 redirectToOAuthLogin 发起完整 PKCE 登录
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

/** 静默续期失败或无 token 时启动 OAuth2 PKCE 授权 redirect。
 *  传递 prompt=login 强制重新认证：此时已确认 refresh_token 不可用，
 *  Auth 服务的 JSESSIONID 即使有效也不应跳过登录页。 */
function redirectToOAuthLogin(returnUrl: string): void {
  const oauthConfig = getAdminOAuthConfig();
  void startOAuthLogin(oauthConfig, returnUrl, { prompt: "login" });
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
        redirectToOAuthLogin(to.fullPath);
        return false;
      }
      // 静默续期成功，继续执行下方的“已登录”逻辑
    }

    // ── 已登录 ──
    if (authStore.isLoggedIn) {
      authStore.ensureRoutes(router);
      if (!authStore.profile) {
        try {
          await authStore.fetchProfile();
        } catch {
          // profile 拉取失败视为登录态失效
          authStore.token = "";
          authStore.refreshToken = "";
          authStore.profile = null;
          authStore.dynamicReady = false;
          localStorage.removeItem(TOKEN_STORAGE_KEY);
          localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
          localStorage.removeItem(MENUS_STORAGE_KEY);
          redirectToOAuthLogin(to.fullPath);
          return false;
        }
      }
      if (routeName === "NotFound" && mayBeDynamicPath) {
        return { path: to.fullPath, replace: true };
      }
    }
  });
}
