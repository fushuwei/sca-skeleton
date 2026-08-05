import type { Router } from "vue-router";
import { refreshAccessToken } from "../oauth/password-grant";
import type { OAuthAppConfig } from "../oauth/password-grant";
import { isNotificationHandled, isTransientNetworkError } from "../oauth/axios-oauth";
import {
  shouldRetryTransient,
  markRetry,
  resetRetry,
  delay,
  RETRY_DELAY_MS,
  type TransientRetryState
} from "./guard-utils";

/**
 * 路由守卫所需的 store 形状（admin / portal store 均满足此接口）。
 *
 * 通过结构化接口解耦守卫逻辑与具体 store 实现，使 {@link createAuthGuard}
 * 可被两个 SPA 共享，消除 ~100 行重复代码。
 */
export interface AuthGuardStore {
  token: string;
  refreshToken: string;
  isLoggedIn: boolean;
  profile: { isSuperadmin?: number } | null;
  menus: unknown[];
  dynamicReady: boolean;
  syncOAuthTokens(accessToken: string, refreshToken?: string): void;
  fetchProfile(): Promise<void>;
  ensureRoutes(router: Router): void;
  resetState(): void;
}

/** 创建路由守卫所需的依赖配置。 */
export interface AuthGuardConfig {
  /** Pinia store 获取函数 */
  useStore: () => AuthGuardStore;
  /** OAuth 客户端配置获取函数 */
  getOAuthConfig: () => OAuthAppConfig;
  /** 无需登录即可访问的路由名称集合 */
  whiteListRouteNames: Set<string>;
  /** 从菜单树中提取叶子路由路径集合 */
  collectLeafMenuPaths: (menus: unknown[]) => string[];
  /** localStorage 中 access_token 的存储键 */
  tokenStorageKey: string;
  /** localStorage 中 refresh_token 的存储键 */
  refreshTokenStorageKey: string;
}

/** 静默续期的判定结果。 */
type SilentRefreshResult = "ok" | "transient" | "invalid";

/**
 * 使用已保存的 refresh_token 静默续期 access_token。
 * 成功时更新 localStorage 与 Pinia store；仅当后端明确拒绝（refresh_token 已失效）时才清除令牌。
 *
 * @returns "ok"        续期成功，可继续导航
 *          "transient" 网络/浏览器中止，保留令牌，调用方应进入重试流程
 *          "invalid"   refresh_token 已失效，调用方应登出
 */
async function trySilentRefresh(config: AuthGuardConfig): Promise<SilentRefreshResult> {
  const refreshToken = localStorage.getItem(config.refreshTokenStorageKey);
  if (!refreshToken) {
    return "invalid";
  }
  try {
    const tokenResponse = await refreshAccessToken(config.getOAuthConfig(), refreshToken);
    localStorage.setItem(config.tokenStorageKey, tokenResponse.access_token);
    if (tokenResponse.refresh_token) {
      localStorage.setItem(config.refreshTokenStorageKey, tokenResponse.refresh_token);
    }
    const store = config.useStore();
    store.syncOAuthTokens(tokenResponse.access_token, tokenResponse.refresh_token);
    return "ok";
  } catch (error) {
    // 网络异常或浏览器中止请求（如 Firefox 快速刷新 abort fetch）：
    // 非 refresh_token 失效，保留令牌让调用方重试
    if (isTransientNetworkError(error)) {
      return "transient";
    }
    // 后端明确拒绝（refresh_token 已失效）：清除令牌
    config.useStore().resetState();
    return "invalid";
  }
}

/**
 * 创建路由守卫（admin / portal 共享）。
 *
 * 守卫职责：
 * 1. 未登录时尝试静默 refresh_token 续期；
 * 2. 已登录但缺少 profile 时拉取用户信息；
 * 3. 对瞬时网络错误（如 Firefox 快速刷新 abort）做有界重试，避免误登出。
 *
 * 瞬时错误处理策略：通过 `return { path: to.fullPath }` 重触发导航以静默重试，
 * 最多 {@link shouldRetryTransient} 允许的次数；重试耗尽后放行导航（用缓存渲染），
 * 绝不因网络错误清令牌或登出——只有后端明确拒绝（refresh_token 失效）才登出。
 */
export function createAuthGuard(config: AuthGuardConfig): (router: Router) => void {
  const transientRetry: TransientRetryState = { count: 0 };

  return function setupRouterGuards(router: Router): void {
    router.beforeEach(async (to) => {
      const authStore = config.useStore();
      const routeName = String(to.name ?? "");
      const isWhiteRoute = config.whiteListRouteNames.has(routeName);
      const leafPaths = config.collectLeafMenuPaths(authStore.menus);
      const mayBeDynamicPath = leafPaths.includes(to.path);

      // ── 未登录：先尝试静默 refresh_token 续期 ──
      if (!authStore.isLoggedIn && !isWhiteRoute) {
        const refreshed = await trySilentRefresh(config);
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
            authStore.resetState();
            return { name: "Login", query: { redirect: to.fullPath } };
          }
        }
        if (routeName === "NotFound" && mayBeDynamicPath) {
          return { path: to.fullPath, replace: true };
        }
      }
    });
  };
}
