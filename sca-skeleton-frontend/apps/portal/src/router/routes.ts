import type { RouteRecordRaw } from "vue-router";
import PortalLayout from "../layouts/PortalLayout.vue";
import OAuthCallbackView from "../views/auth/OAuthCallbackView.vue";
import NotFoundView from "../views/error/NotFoundView.vue";

/** 无需登录即可访问的路由名称（OAuth 回调页） */
export const WHITE_LIST_ROUTE_NAMES = new Set(["OAuthCallback"]);

export const staticRoutes: RouteRecordRaw[] = [
  {
    path: "/oauth/callback",
    name: "OAuthCallback",
    component: OAuthCallbackView,
    meta: { public: true, title: "登录回调" }
  },
  {
    path: "/",
    name: "Root",
    component: PortalLayout,
    redirect: "/portal/home",
    meta: { requiresAuth: true, title: "数据中台" },
    children: []
  },
  {
    path: "/:pathMatch(.*)*",
    name: "NotFound",
    component: NotFoundView,
    meta: { public: true, title: "页面不存在" }
  }
];
