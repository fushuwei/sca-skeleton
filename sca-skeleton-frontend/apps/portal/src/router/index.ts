import { createRouter, createWebHistory } from "vue-router";
import { startOAuthLogin } from "@repo/shared";
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

router.beforeEach(async (to, _from, next) => {
  const authStore = usePortalAuthStore();
  const isPublic = to.meta.public === true;
  if (!authStore.isLoggedIn && !isPublic) {
    void startOAuthLogin(getPortalOAuthConfig(), to.fullPath);
    next(false);
    return;
  }
  if (authStore.isLoggedIn && !isPublic && !authStore.profile) {
    try {
      await authStore.fetchProfile();
    } catch {
      authStore.token = "";
      authStore.refreshToken = "";
      authStore.profile = null;
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
      void startOAuthLogin(getPortalOAuthConfig(), to.fullPath);
      next(false);
      return;
    }
  }
  next();
});
