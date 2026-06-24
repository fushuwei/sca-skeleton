import { createRouter, createWebHistory } from "vue-router";
import { refreshAccessToken, startOAuthLogin } from "@repo/shared";
import { getPortalOAuthConfig } from "../config/oauth";
import { usePortalAuthStore } from "../stores/auth";
import {
  REFRESH_TOKEN_STORAGE_KEY,
  TOKEN_STORAGE_KEY
} from "../constants/auth-storage";

const routes = [
  {
    path: "/oauth/callback",
    name: "OAuthCallback",
    component: () => import("../views/auth/OAuthCallbackView.vue"),
    meta: { public: true }
  },
  {
    path: "/",
    name: "Home",
    component: () => import("../views/Home.vue"),
    meta: { requiresAuth: true }
  }
];

export const router = createRouter({
  history: createWebHistory(),
  routes
});

/**
 * 使用已保存的 refresh_token 静默续期 access_token。
 * 成功时更新 localStorage 与 Pinia store，失败时清除全部令牌。
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
    const authStore = usePortalAuthStore();
    authStore.token = "";
    authStore.refreshToken = "";
    return false;
  }
}

router.beforeEach(async (to, _from, next) => {
  const authStore = usePortalAuthStore();
  const isPublic = to.meta.public === true;

  // ── 未登录：先尝试静默 refresh_token 续期 ──
  if (!authStore.isLoggedIn && !isPublic) {
    const refreshed = await trySilentRefresh();
    if (!refreshed) {
      // 续期失败或无 token：完整 PKCE 登录，强制重新认证
      void startOAuthLogin(getPortalOAuthConfig(), to.fullPath, { prompt: "login" });
      next(false);
      return;
    }
    // 静默续期成功，继续执行下方的"已登录"逻辑
  }

  // ── 已登录 ──
  if (authStore.isLoggedIn && !isPublic && !authStore.profile) {
    try {
      await authStore.fetchProfile();
    } catch {
      authStore.token = "";
      authStore.refreshToken = "";
      authStore.profile = null;
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
      void startOAuthLogin(getPortalOAuthConfig(), to.fullPath, { prompt: "login" });
      next(false);
      return;
    }
  }
  next();
});
