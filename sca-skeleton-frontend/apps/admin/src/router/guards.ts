import type { Router } from "vue-router";
import { refreshAccessToken, isNotificationHandled } from "@repo/shared";
import {
  isTransientNetworkError,
  shouldRetryTransient,
  markRetry,
  resetRetry,
  delay,
  RETRY_DELAY_MS,
  type TransientRetryState
} from "@repo/shared";
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
 * 跨同一导航周期内多次 beforeEach 调用共享的重试计数。
 * 瞬时网络错误时守卫通过 `return { path: to.fullPath }` 重触发导航以静默重试，
 * 计数用于限制重试轮数，避免无限循环。
 */
const transientRetry: TransientRetryState = { count: 0 };

/** 静默续期的判定结果。 */
type SilentRefreshResult = "ok" | "transient" | "invalid";

/** 清除令牌与内存态（登录失效时调用）。 */
function clearTokensAndState(authStore: ReturnType<typeof useAuthStore>): void {
  authStore.token = "";
  authStore.refreshToken = "";
  authStore.profile = null;
  authStore.menus = [];
  authStore.dynamicReady = false;
  localStorage.removeItem(TOKEN_STORAGE_KEY);
  localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
  localStorage.removeItem(MENUS_STORAGE_KEY);
}

/**
 * 使用已保存的 refresh_token 静默续期 access_token。
 * 成功时更新 localStorage 与 Pinia store；仅当后端明确拒绝（refresh_token 已失效）时才清除令牌。
 *
 * @returns "ok"        续期成功，可继续导航
 *          "transient" 网络/浏览器中止，保留令牌，调用方应进入重试流程
 *          "invalid"   refresh_token 已失效，调用方应登出
 */
async function trySilentRefresh(): Promise<SilentRefreshResult> {
  const refreshToken = localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY);
  if (!refreshToken) {
    return "invalid";
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
    return "ok";
  } catch (error) {
    // 网络异常或浏览器中止请求（如 Firefox 快速刷新 abort fetch）：
    // 非 refresh_token 失效，保留令牌让调用方重试
    if (isTransientNetworkError(error)) {
      return "transient";
    }
    // 后端明确拒绝（refresh_token 已失效）：清除令牌
    clearTokensAndState(useAuthStore());
    return "invalid";
  }
}

export function setupRouterGuards(router: Router): void {
  router.beforeEach(async (to) => {
    const authStore = useAuthStore();
    const routeName = String(to.name ?? "");
    const isWhiteRoute = WHITE_LIST_ROUTE_NAMES.has(routeName);
    const leafPaths = collectLeafMenuPaths(authStore.menus);
    const mayBeDynamicPath = leafPaths.includes(to.path);

    // ── 未登录：先尝试静默 refresh_token 续期 ──
    if (!authStore.isLoggedIn && !isWhiteRoute) {
      const refreshed = await trySilentRefresh();
      if (refreshed === "transient") {
        // 网络抖动/浏览器中止：保留令牌做有限次重试，避免误登出
        if (shouldRetryTransient(transientRetry)) {
          markRetry(transientRetry);
          await delay(RETRY_DELAY_MS);
          return { path: to.fullPath, replace: true };
        }
        resetRetry(transientRetry);
        // 重试耗尽仍无法续期：跳转登录页（令牌保留，下次进入再尝试）
        return { name: "Login", query: { redirect: to.fullPath } };
      }
      if (refreshed === "invalid") {
        resetRetry(transientRetry);
        return { name: "Login", query: { redirect: to.fullPath } };
      }
      // refreshed === "ok"：续期成功，重置重试计数后继续"已登录"逻辑
      resetRetry(transientRetry);
    }

    // ── 已登录但访问登录页：重定向到首页 ──
    if (authStore.isLoggedIn && routeName === "Login") {
      resetRetry(transientRetry);
      return { name: "Root" };
    }

    // ── 已登录 ──
    if (authStore.isLoggedIn) {
      authStore.ensureRoutes(router);
      // profile 为空或缺少 isSuperadmin 字段（兼容旧版 profile 缓存）时重新拉取
      if (!authStore.profile || authStore.profile.isSuperadmin === undefined) {
        try {
          await authStore.fetchProfile();
          resetRetry(transientRetry);
        } catch (error) {
          // 网络异常或浏览器中止请求（如 Firefox 快速刷新 abort fetch）：
          // 非认证失败，保留令牌做有限次重试；重试耗尽后放行导航，避免误登出与白屏
          if (isTransientNetworkError(error)) {
            if (shouldRetryTransient(transientRetry)) {
              markRetry(transientRetry);
              await delay(RETRY_DELAY_MS);
              return { path: to.fullPath, replace: true };
            }
            resetRetry(transientRetry);
            // 持续网络故障：保留令牌放行，用缓存菜单渲染（profile 缺失时布局有兜底文案）
            return true;
          }
          // 已弹提示的真实 HTTP 错误（如 500/403/429）：不是登录失效，保留令牌并放行
          if (isNotificationHandled(error)) {
            resetRetry(transientRetry);
            return true;
          }
          // 真正的认证失败（401 且 refresh 也失败）：清除令牌，重定向到登录页
          resetRetry(transientRetry);
          clearTokensAndState(authStore);
          return { name: "Login", query: { redirect: to.fullPath } };
        }
      }
      if (routeName === "NotFound" && mayBeDynamicPath) {
        return { path: to.fullPath, replace: true };
      }
    }
  });
}
