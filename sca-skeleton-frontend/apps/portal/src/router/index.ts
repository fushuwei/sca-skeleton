import { createRouter, createWebHistory } from "vue-router";
import { startOAuthLogin } from "@repo/shared";
import { getPortalOAuthConfig } from "../config/oauth";
import { usePortalAuthStore } from "../stores/auth";

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

router.beforeEach((to, _from, next) => {
  const authStore = usePortalAuthStore();
  const isPublic = to.meta.public === true;
  if (!authStore.isLoggedIn && !isPublic) {
    void startOAuthLogin(getPortalOAuthConfig(), to.fullPath);
    next(false);
    return;
  }
  next();
});
