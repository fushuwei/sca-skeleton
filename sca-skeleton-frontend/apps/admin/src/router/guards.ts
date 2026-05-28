import type { NavigationGuardNext, RouteLocationNormalized, Router } from "vue-router";
import { startOAuthLogin } from "@repo/shared";
import { getAdminOAuthConfig } from "../config/oauth";
import { WHITE_LIST_ROUTE_NAMES } from "./routes";
import { useAuthStore } from "../stores/auth";
import { collectLeafMenuPaths } from "../utils/menu-tree";
import {
  MENUS_STORAGE_KEY,
  REFRESH_TOKEN_STORAGE_KEY,
  TOKEN_STORAGE_KEY
} from "../constants/auth-storage";

/** 未登录时启动 OAuth2 PKCE 授权 redirect（跳转 Auth 服务登录页）。 */
function redirectToOAuthLogin(returnUrl: string): void {
  const oauthConfig = getAdminOAuthConfig();
  void startOAuthLogin(oauthConfig, returnUrl);
}

export function setupRouterGuards(router: Router): void {
  router.beforeEach(async (to, _from, next) => {
    const authStore = useAuthStore();
    const routeName = String(to.name ?? "");
    const isWhiteRoute = WHITE_LIST_ROUTE_NAMES.has(routeName);
    const leafPaths = collectLeafMenuPaths(authStore.menus);
    const mayBeDynamicPath = leafPaths.includes(to.path);

    if (!authStore.isLoggedIn && !isWhiteRoute) {
      redirectToOAuthLogin(to.fullPath);
      next(false);
      return;
    }

    if (authStore.isLoggedIn) {
      authStore.ensureRoutes(router);
      if (!authStore.profile) {
        try {
          await authStore.fetchProfile();
        } catch {
          // profile 拉取失败时视为本地登录态失效，清理后重走 OAuth 登录
          authStore.token = "";
          authStore.refreshToken = "";
          authStore.profile = null;
          authStore.dynamicReady = false;
          localStorage.removeItem(TOKEN_STORAGE_KEY);
          localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
          localStorage.removeItem(MENUS_STORAGE_KEY);
          redirectToOAuthLogin(to.fullPath);
          next(false);
          return;
        }
      }
      if (routeName === "NotFound" && mayBeDynamicPath) {
        next({ path: to.fullPath, replace: true });
        return;
      }
    }

    next();
  });
}
