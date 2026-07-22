import type { Router } from "vue-router";
import { refreshAccessToken, startOAuthLogin } from "@repo/shared";
import { getPortalOAuthConfig } from "../config/oauth";
import { WHITE_LIST_ROUTE_NAMES } from "./routes";
import { usePortalAuthStore } from "../stores/auth";
import { collectLeafMenuPaths } from "../utils/menu-tree";
import {
  MENUS_STORAGE_KEY,
  REFRESH_TOKEN_STORAGE_KEY,
  TOKEN_STORAGE_KEY
} from "../constants/auth-storage";

/**
 * 使用已保存的 refresh_token 静默续期 access_token。
 * 成功时更新 localStorage 与 Pinia store，失败时清除全部令牌与菜单缓存。
 */
async function trySilentRefresh(): Promise<boolean> {
  const refreshToken = localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY);
  if (!refreshToken) {
    return false;
  }
  try {
    const oauthConfig = getPortalOAuthConfig();
    const tokenResponse = await refreshAccessToken(oauthConfig, refreshToken);
    localStorage.setItem(TOKEN_STORAGE_KEY, tokenResponse.access_token);
    if (tokenResponse.refresh_token) {
      localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, tokenResponse.refresh_token);
    }
    const authStore = usePortalAuthStore();
    authStore.syncOAuthTokens(tokenResponse.access_token, tokenResponse.refresh_token);
    return true;
  } catch {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
    localStorage.removeItem(MENUS_STORAGE_KEY);
    const authStore = usePortalAuthStore();
    authStore.token = "";
    authStore.refreshToken = "";
    authStore.menus = [];
    authStore.dynamicReady = false;
    return false;
  }
}

/**
 * 静默续期失败或无 token 时启动 OAuth2 PKCE 授权 redirect。
 */
function redirectToOAuthLogin(returnUrl: string, hadRefreshToken: boolean): void {
  const oauthConfig = getPortalOAuthConfig();
  void startOAuthLogin(oauthConfig, returnUrl, hadRefreshToken ? { prompt: "login" } : undefined);
}

export function setupRouterGuards(router: Router): void {
  router.beforeEach(async (to, _from) => {
    const authStore = usePortalAuthStore();
    const routeName = String(to.name ?? "");
    const isWhiteRoute = WHITE_LIST_ROUTE_NAMES.has(routeName);
    const leafPaths = collectLeafMenuPaths(authStore.menus);
    const mayBeDynamicPath = leafPaths.includes(to.path);

    // ── 未登录：先尝试静默 refresh_token 续期 ──
    if (!authStore.isLoggedIn && !isWhiteRoute) {
      const hadRefreshToken = Boolean(localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY));
      const refreshed = await trySilentRefresh();
      if (!refreshed) {
        redirectToOAuthLogin(to.fullPath, hadRefreshToken);
        return false;
      }
      // 静默续期成功，继续执行下方的"已登录"逻辑
    }

    // ── 已登录 ──
    if (authStore.isLoggedIn) {
      authStore.ensureRoutes(router);
      // profile 为空或缺少 isSuperadmin 字段（兼容旧版 profile 缓存）时重新拉取
      if (!authStore.profile || authStore.profile.isSuperadmin === undefined) {
        try {
          await authStore.fetchProfile();
        } catch {
          // profile 拉取失败视为登录态失效（此时用户曾持有 token，故强制 prompt=login 重登）
          authStore.token = "";
          authStore.refreshToken = "";
          authStore.profile = null;
          authStore.dynamicReady = false;
          localStorage.removeItem(TOKEN_STORAGE_KEY);
          localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
          localStorage.removeItem(MENUS_STORAGE_KEY);
          redirectToOAuthLogin(to.fullPath, true);
          return false;
        }
      }
      if (routeName === "NotFound" && mayBeDynamicPath) {
        return { path: to.fullPath, replace: true };
      }
    }
  });
}
